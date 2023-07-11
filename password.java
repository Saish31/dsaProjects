package dsaprojects;

import java.util.Random;
import java.util.Scanner;

public class password {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter the desired password length: ");
        int length = sc.nextInt();
        sc.nextLine();

        System.out.print("Include numbers? (Y/N): ");
        boolean includeNumbers = sc.nextLine().equalsIgnoreCase("Y");

        System.out.print("Include symbols? (Y/N): ");
        boolean includeSymbols = sc.nextLine().equalsIgnoreCase("Y");

        String generatedPassword = generatePassword(length, includeNumbers, includeSymbols);
        System.out.println("Generated Password: " + generatedPassword);
    }

    private static String generatePassword(int length, boolean includeNumbers, boolean includeSymbols) {
        String lowercaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String uppercaseLetters = lowercaseLetters.toUpperCase();
        String numbers = "0123456789";
        String symbols = "!@#$%^&*()_+[]{}<>?";

        StringBuilder password = new StringBuilder();
        Random random = new Random();

        String characters = lowercaseLetters + uppercaseLetters;
        if (includeNumbers) {
            characters += numbers;
        }
        if (includeSymbols) {
            characters += symbols;
        }

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            password.append(characters.charAt(index));
        }

        return password.toString();
    }
}
