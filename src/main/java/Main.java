import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) throws FileNotFoundException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

     final String firstCommand = args[0];

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
         switch(args[1]){
           case "-p" -> {
               String objectHash = args[2];

               String dir = "./.git/objects/" + objectHash.substring(0, 2) + "/";

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
                 System.out.print(content.substring(content.indexOf("\0") + 1));

               } catch (IOException e) {
                 throw new RuntimeException(e);
               }
           }
         }
       }
       case "hash-object" -> {
         switch(args[1]){
           case "-w" -> {
            String fileName = args[2];
            String objectHash = getObjectHash(fileName);
            String grandParentDirPath = "./.git/objects";
            File grandParentDir = new File(grandParentDirPath);

            String parentPath = objectHash.substring(0, 2);
            File parentDir = new File(grandParentDir, parentPath);

            File filePath = new File(parentDir, objectHash.substring(2));

            try {
              filePath.createNewFile();
              System.out.println(objectHash);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
           }
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
