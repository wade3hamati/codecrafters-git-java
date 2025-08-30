import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) throws IOException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.

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
             File file = new File(fileName);

             if(!file.exists()){
               throw new RuntimeException("File does not exist");
             }
             byte[] content = Files.readAllBytes(file.toPath());
             byte[] header = ("blob " + content.length + "\0").getBytes();
             byte [] result = new byte[content.length + header.length];
             System.arraycopy(header, 0, result, 0, header.length);
             System.arraycopy(content, 0, result, header.length, content.length);

             String objectHash = getObjectHash(result);

             String grandParentDirPath = "./.git/objects/" + objectHash.substring(0,2) + "/";
             String filePath = grandParentDirPath + objectHash.substring(2);
             new File(filePath).mkdirs();

             try (FileOutputStream fos = new FileOutputStream(filePath);
                  DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
               dos.write(result);
               System.out.print(objectHash);
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

  public static String getObjectHash(byte[] input){
    try{
      MessageDigest md = MessageDigest.getInstance("SHA-1");

      byte[] messageDigest = md.digest(input);

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
