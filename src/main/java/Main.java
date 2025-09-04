import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) throws IOException {

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

           System.out.print(createBlob(file));
         }
       }
     }
     case "ls-tree" -> {

       String objectHash = args[2];

       String dir = "./.git/objects/" + objectHash.substring(0, 2) + "/";
       File compressedFilePath = new File(dir + objectHash.substring(2));

       try (FileInputStream fis = new FileInputStream(compressedFilePath);
            InflaterInputStream iis = new InflaterInputStream(fis)) {

         byte[] decompressed = iis.readAllBytes();

         int nullIndex = 0;
         while (decompressed[nullIndex] != 0) {
           nullIndex++;
         }

         int i = nullIndex + 1;

         while ( i < decompressed.length) {
           int spaceIndex = i;
           while(decompressed[spaceIndex] != ' '){
             spaceIndex++;
           }
           String mode = new String(decompressed, i, spaceIndex - i);

           i = spaceIndex + 1;
           int nullTerminator = i;
           while(decompressed[nullTerminator] != 0){
             nullTerminator++;
           }
           String filename = new String(decompressed, i, nullTerminator - i);
           i = nullTerminator + 1;

           byte[] shaBytes = Arrays.copyOfRange(decompressed, i, i + 20);
           StringBuilder shaHex = new StringBuilder();
           for (byte b : shaBytes) {
             shaHex.append(String.format("%02x", b));
           }
           i += 20;

           System.out.printf("%s%n", filename);
         }

       } catch (IOException e) {
         throw new RuntimeException(e);
       }
     }
     case "write-tree" -> {
       Path dir = Paths.get("./");
       String treeHash = createTree(dir.toFile());
       System.out.println(treeHash);
     }
     case "commit-tree" -> {
       switch(args[2]){
         case "-p" -> {
           String treeSha = args[1];
           String commitSha = args[3];

           ByteArrayOutputStream content = new ByteArrayOutputStream();

           String treeEntry = "tree " + treeSha + "\n";
           byte[] treeEntryBytes = treeEntry.getBytes();
           content.write(treeEntryBytes);

           String parentEntry = "parent " + commitSha + "\n";
           byte[] parentEntryBytes = parentEntry.getBytes();
           content.write(parentEntryBytes);

           String authorEntry = "author Wadeh Hamati <wade3_hamati@outlook.com> 1243040974 -0700\n";
           byte[] authorEntryBytes = authorEntry.getBytes();
           content.write(authorEntryBytes);

           String committerEntry = "committer Wadeh Hamati <wade3_hamati@outlook.com> 1243040974 -0700\n";
           byte[] committerEntryBytes = committerEntry.getBytes();
           content.write(committerEntryBytes);
           content.write((byte) '\n');

           String commitMessage = args[5].substring(0,args[5].length());
           byte[] commitMessageBytes = commitMessage.getBytes();
           content.write(commitMessageBytes);
           content.write((byte) '\n');

           byte[] body = content.toByteArray();
           String header = "commit " + body.length + "\0";
           ByteArrayOutputStream finalObject = new ByteArrayOutputStream();
           finalObject.write(header.getBytes());
           finalObject.write(body);

           byte[] finalBytes = finalObject.toByteArray();

           String commitHash = getObjectHash40(finalBytes);

           String parentDir = "./.git/objects/" + commitHash.substring(0, 2);
           String filePath = parentDir + "/" + commitHash.substring(2);

           File dir = new File(parentDir);
           if (!dir.exists() && !dir.mkdirs()) {
             throw new IOException("Failed to create directory: " + parentDir);
           }

           File objectFile = new File(filePath);
           try (FileOutputStream fos = new FileOutputStream(objectFile);
                DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
             dos.write(finalObject.toByteArray());
           }
           System.out.println(commitHash);
         }
       }
     }
     default -> System.out.println("Unknown Command: " + firstCommand);
    }
  }

  public static String getObjectHash40( byte[] input){
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

  public static String createBlob(File file) throws IOException {
    if(!file.exists()){
      throw new RuntimeException("File does not exist");
    }
    byte[] content = Files.readAllBytes(file.toPath());
    byte[] header = ("blob " + content.length + "\0").getBytes();
    byte [] result = new byte[content.length + header.length];
    System.arraycopy(header, 0, result, 0, header.length);
    System.arraycopy(content, 0, result, header.length, content.length);

    String objectHash = getObjectHash40(result);

    String dirPath = "./.git/objects/" + objectHash.substring(0, 2);
    String filePath = dirPath + "/" + objectHash.substring(2);

    File dir = new File(dirPath);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Failed to create directory: " + dirPath);
    }

    File objectFile = new File(filePath);
    if (objectFile.exists()) {
      return objectHash;
    }
    try (FileOutputStream fos = new FileOutputStream(objectFile);
         DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
      dos.write(result);
    }

    return objectHash;
  }

  public static String createTree(File file) throws IOException {

    if(!file.exists()){
      throw new RuntimeException("File does not exist");
    }

    ArrayList<Path> paths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(file.toPath())) {
       for (Path path : stream) {
         if(Objects.equals(path.getFileName().toString(), ".git") ||
                 Objects.equals(path.getFileName().toString(), ".idea")){
           continue;
         }
         paths.add(path);
       }
    }
    paths.sort(Comparator.comparing(p -> p.getFileName().toString()));

    ByteArrayOutputStream content = new ByteArrayOutputStream();

    for (Path path : paths) {
      String mode;
      byte[] shaBytes;

      if (Files.isDirectory(path)) {
        if(Objects.equals(path.getFileName().toString(), ".git") ||
                Objects.equals(path.getFileName().toString(), ".idea")){
          continue;
        }
        String subtreeHash = createTree(path.toFile());
        shaBytes = hexToBytes(subtreeHash);
        mode = "40000"; // directory
      } else {
        String blobHash = createBlob(path.toFile());
        shaBytes = hexToBytes(blobHash);
        mode = "100644"; // regular file
      }

      content.write(mode.getBytes());
      content.write(' ');

      content.write(path.getFileName().toString().getBytes());
      content.write(0);

      content.write(shaBytes);
    }


    byte[] body = content.toByteArray();
    byte[] header = ("tree " + body.length + "\0").getBytes();

    byte[] result = new byte[body.length + header.length];
    System.arraycopy(header, 0, result, 0, header.length);
    System.arraycopy(body, 0, result, header.length, body.length);

    String objectHash = getObjectHash40(result);

    String dirPath = "./.git/objects/" + objectHash.substring(0, 2);
    String filePath = dirPath + "/" + objectHash.substring(2);

    File dir = new File(dirPath);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Failed to create directory: " + dirPath);
    }

    File objectFile = new File(filePath);
    if (objectFile.exists()) {
      return objectHash;
    }
    try (FileOutputStream fos = new FileOutputStream(objectFile);
         DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
      dos.write(result);
    }

    return objectHash;
  }

  public static byte[] hexToBytes(String hex) {
    if (hex.length() != 40) {
      throw new IllegalArgumentException("Expected 40-character SHA-1 hex string");
    }
    byte[] bytes = new byte[20];
    for (int i = 0; i < 20; i++) {
      bytes[i] = (byte) ((Character.digit(hex.charAt(i*2), 16) << 4)
              + Character.digit(hex.charAt(i*2 + 1), 16));
    }
    return bytes;
  }

}
