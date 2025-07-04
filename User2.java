package group_chatting_application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class User2 implements ActionListener,Runnable{
    
    JTextField text1;
    JPanel a1;
    static Box vertical=Box.createVerticalBox();
    static JFrame f=new JFrame();
    static DataOutputStream dout;

    BufferedReader reader;
    BufferedWriter writer;
    String name="Suarez";

    User2(){
        f.setLayout(null);

        JPanel p1=new JPanel();
        p1.setBackground(new Color(51,153,255));
        p1.setBounds(0,0, 450,70);
        p1.setLayout(null);
        f.add(p1);

        ImageIcon i1 = new ImageIcon(getClass().getResource("icons/back_arrow.png"));
        Image i2=i1.getImage().getScaledInstance(25,25, Image.SCALE_SMOOTH);
        ImageIcon i3=new ImageIcon(i2);
        JLabel back=new JLabel(i3);
        back.setBounds(5,20,25,25);
        p1.add(back);
        back.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent ae){
                System.exit(0);
            }
        });

        ImageIcon i4=new ImageIcon(getClass().getResource("icons/msn.png"));
        Image i5=i4.getImage().getScaledInstance(50,50, Image.SCALE_SMOOTH);
        ImageIcon i6=new ImageIcon(i5);
        JLabel pfp=new JLabel(i6);
        pfp.setBounds(40,10,50,50);
        p1.add(pfp);

        ImageIcon i7=new ImageIcon(getClass().getResource("icons/video.png"));
        Image i8=i7.getImage().getScaledInstance(30,30, Image.SCALE_SMOOTH);
        ImageIcon i9=new ImageIcon(i8);
        JLabel video=new JLabel(i9);
        video.setBounds(300,20,30,30);
        p1.add(video);

        ImageIcon i10=new ImageIcon(getClass().getResource("icons/phone.png"));
        Image i11=i10.getImage().getScaledInstance(35,30, Image.SCALE_SMOOTH);
        ImageIcon i12=new ImageIcon(i11);
        JLabel phone=new JLabel(i12);
        phone.setBounds(360,20,35,30);
        p1.add(phone);

        ImageIcon i13=new ImageIcon(getClass().getResource("icons/more.png"));
        Image i14=i13.getImage().getScaledInstance(10,25, Image.SCALE_SMOOTH);
        ImageIcon i15=new ImageIcon(i14);
        JLabel more=new JLabel(i15);
        more.setBounds(420,20,10,25);
        p1.add(more);

        JLabel name=new JLabel("MSN");
        name.setBounds(110,15,100,18);
        name.setForeground(Color.WHITE);
        name.setFont(new Font("SAN_SERIF",Font.BOLD,18));
        p1.add(name);

        JLabel status=new JLabel("Suarez, Messi, Neymar");
        status.setBounds(110,35,160,18);
        status.setForeground(Color.WHITE);
        status.setFont(new Font("SAN_SERIF",Font.BOLD,14));
        p1.add(status);

        a1=new JPanel();
        a1.setBounds(5,75,440,485);
        a1.setBackground(Color.WHITE);
        a1.setLayout(new BorderLayout()); 
        f.add(a1);

        text1=new JTextField();
        text1.setBounds(5,560,310,40);
        text1.setFont(new Font("SAN_SERIF",Font.PLAIN,16));
        f.add(text1);

        JButton send=new JButton("Send");
        send.setBounds(315,560,130,40);
        send.setBackground(new Color(51,153,255));
        send.setForeground(Color.WHITE);
        send.addActionListener(this);
        send.setFont(new Font("SAN_SERIF",Font.PLAIN,16));
        f.add(send);

        f.setSize(450,600);
        f.setLocation(490,45);
        f.setUndecorated(true);
        f.getContentPane().setBackground(Color.WHITE);

        f.setVisible(true);

        try{
            Socket socket=new Socket("localhost",2003);
            writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent ae){
        try{
            String msgText = text1.getText().trim();
            if (msgText.isEmpty()) return;

            String out = name + ": " + msgText;

            JPanel line = formatMessage(name, msgText, true);
            vertical.add(line);
            vertical.add(Box.createVerticalStrut(15));
            a1.add(vertical, BorderLayout.PAGE_START);

            writer.write(out);
            writer.newLine();
            writer.flush();

            text1.setText("");
            f.revalidate();
            f.repaint();
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }

    public JPanel formatMessage(String sender, String text, boolean isSelf){
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(isSelf ? new Color(51, 204, 255) : new Color(230, 230, 230));

        JLabel nameLabel = new JLabel(sender);
        nameLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
        nameLabel.setBorder(new EmptyBorder(0, 5, 2, 5));

        JLabel msgLabel = new JLabel(
            "<html><p style=\"width:150px\">" + text + "</p></html>"
        );
        msgLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
        msgLabel.setOpaque(true);
        msgLabel.setBackground(isSelf ? new Color(51, 204, 255) : new Color(230, 230, 230));
        msgLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel timeLabel = new JLabel(
            new SimpleDateFormat("HH:mm").format(new Date())
        );
        timeLabel.setFont(new Font("Tahoma", Font.PLAIN, 10));
        timeLabel.setBorder(new EmptyBorder(2, 5, 0, 5));

        bubble.add(nameLabel);
        bubble.add(msgLabel);
        bubble.add(timeLabel);

        JLabel avatar = new JLabel(loadAvatar(sender));

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(new EmptyBorder(5, 5, 5, 5));

        if(isSelf){
            container.add(bubble, BorderLayout.CENTER);
            container.add(avatar, BorderLayout.EAST);
        }else{
            container.add(avatar, BorderLayout.WEST);
            container.add(bubble, BorderLayout.CENTER);
        }

        return container;
    }


    public void run(){
        try{
            String raw;
            while((raw = reader.readLine()) != null){
                String[] parts = raw.split(":", 2);
                if (parts.length < 1) continue;
                String sender = parts[0].trim();
                String body = parts.length > 1 ? parts[1].trim() : "";
                if (sender.equals(this.name)) continue;

                JPanel line = formatMessage(sender, body, false);
                SwingUtilities.invokeLater(() -> {
                    vertical.add(line);
                    vertical.add(Box.createVerticalStrut(15));
                    a1.add(vertical, BorderLayout.PAGE_START);
                    f.revalidate();
                    f.repaint();
                });
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private ImageIcon loadAvatar(String senderName){
        String resource;
        switch (senderName) {
            case "Messi":  resource = "/group_chatting_application/icons/messi.png";  break;
            case "Suarez": resource = "/group_chatting_application/icons/suarez.png"; break;
            case "Neymar": resource = "/group_chatting_application/icons/neymar.png"; break;
            default:       resource = "/group_chatting_application/icons/defaultpic.png"; break;
        }
        ImageIcon original = new ImageIcon(getClass().getResource(resource));
        Image scaled = original.getImage()
                             .getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static void main(String[] args){
        User2 two=new User2();
        Thread t1=new Thread(two);
        t1.start();
    }
}