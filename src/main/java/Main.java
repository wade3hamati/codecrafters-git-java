import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) throws FileNotFoundException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

//     Uncomment this block to pass the first stage

     final String firstCommand = args[0];
     final String secondCommand = args[1];
     final String thirdCommand = args[2];

     switch (firstCommand) {
       case "init" -> {
         final File root = new File(".git");
         new File(root, "objects").mkdirs();
         new File(root, "refs").mkdirs();
         final File head = new File(root, "HEAD");

         try {
           head.createNewFile();
           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
           System.out.println("Initialized git directory");
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
       }
       case "cat-file" -> {

         switch(secondCommand){

           case "-p" -> {
             String objectHash = getObjectHash(thirdCommand);

             String dir = ".git/objects/" + objectHash.substring(0,2);

             File compressedFilePath = new File(dir + objectHash.substring(2));

             StringBuilder content = new StringBuilder();

             try (FileInputStream fis = new FileInputStream(compressedFilePath);
                  InflaterInputStream iis = new InflaterInputStream(fis);
                  InputStreamReader isr = new InputStreamReader(iis);
                  BufferedReader br = new BufferedReader(isr)) {

               String line;
               while ((line = br.readLine()) != null) {
                 content.append(line);
               }
             } catch (IOException e) {
                 throw new RuntimeException(e);
             }
           }

           case "-t" -> {

           }

           case "-s" -> {

           }
         }
         String objectHash = getObjectHash(secondCommand);

         String parentPath = ".git/objects/";
         String subDirName = objectHash.substring(0,2);

         File parent = new File(".git/objects/", subDirName);
         parent.mkdirs();
         File newObj = new File(parent, objectHash.substring(2));
//         String newObjContent = "blob" +
         try{
           newObj.createNewFile();
//           Files.write(newObj.toPath(), "blob ");
         }catch (IOException e){
           throw new RuntimeException(e);
         }



       }
       default -> System.out.println("Unknown Command: " + firstCommand);
     }
  }

  // question: primitive type vs non-primitive type passing into function
  // question: string vs string buffer vs StringBuilder

  public static String getObjectHash(String input){
    try{
      MessageDigest md = MessageDigest.getInstance("SHA-1");

      byte[] messageDigest = md.digest(input.getBytes());

      BigInteger no = new BigInteger(1, messageDigest);

      StringBuilder hashtext = new StringBuilder(no.toString(16));

      while (hashtext.length() < 40) {
        hashtext.insert(0, "0");
      }

      return hashtext.toString();
    }
    catch (NoSuchAlgorithmException e){
      throw new RuntimeException(e);
    }
  }
}
