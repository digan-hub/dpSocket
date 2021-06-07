import javax.sound.sampled.Line;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class JSONBuilder
{
    private static ArrayList<String> plugins;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException
    {
        plugins = new ArrayList<>();
        String files;
        File f = new File(System.getProperty("user.dir"));
        files = f.getParentFile().getAbsolutePath();
        Path dir = Paths.get(files);
        Files.walk(dir).forEach(path -> showFile(path.toFile()));
        createJSON();
    }

    public static void showFile(File file)
    {
        if (!file.isDirectory())
        {
            if(file.getAbsolutePath().endsWith("Plugin.java"))
            {
                plugins.add(file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".java")).substring(file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".java")).indexOf("plugins\\socket\\")+15).replaceAll("\\\\", "."));
                System.out.println("Plugin: " + file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".java")).substring(file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(".java")).indexOf("plugins\\socket\\")+15).replaceAll("\\\\", "."));
            }
        }
    }

    public static String createHash(String file) throws NoSuchAlgorithmException, IOException
    {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();

        byte[] hash = digest.digest();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static String getHash(MessageDigest digest, File file) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };
        fis.close();
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static void createJSON() throws IOException, NoSuchAlgorithmException
    {
        File rootDirectory = new File(System.getProperty("user.dir")).getParentFile();
        File subDirectory = new File(rootDirectory.getAbsoluteFile() + "/plugins/build/libs/");
        File JSONFile = new File(subDirectory+"\\mainfest");
        File SocketFile = new File(subDirectory+"/Socket.jar");
        if(SocketFile.exists())
        {
            SocketFile.delete();
        }
        File newSocketFile = new File(subDirectory+"/plugins-1.0.jar");
        if(newSocketFile.exists())
        {
            Files.move(Paths.get(newSocketFile.getAbsolutePath()), Paths.get(subDirectory+"/Socket.jar"));
        }
        if(JSONFile.exists())
        {
            JSONFile.delete();
        }
        JSONFile.createNewFile();
        BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(JSONFile.getAbsoluteFile(), true));
        jsonWriter.write("{\n");
        jsonWriter.write("\t\"artifacts\": [\n");
        jsonWriter.write("\t\t{\n");
        jsonWriter.write("\t\t\t\"name\":\"Socket.jar\",\n");
        jsonWriter.write("\t\t\t\t\"plugins\":[\n");
        for(String pluginName : plugins)
        {
            if(plugins.indexOf(pluginName) == plugins.size()-1)
            {
                jsonWriter.write("\t\t\t\t\t\"net.runelite.client.plugins.socket." + pluginName + "\"\n");
            }
            else
            {
                jsonWriter.write("\t\t\t\t\t\"net.runelite.client.plugins.socket." + pluginName + "\",\n");
            }
        }
        jsonWriter.write("\t\t\t\t],\n");
        jsonWriter.write("\t\t\t\"version\": \"3.0.0\",\n");
        jsonWriter.write("\t\t\t\"path\": \"https://raw.githubusercontent.com/c13-c/Socket/master/release/Socket.jar\",\n");
        MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
        String hash = getHash(shaDigest, new File(subDirectory+"\\Socket.jar"));
        jsonWriter.write("\t\t\t\"hash\": \"" + hash + "\",\n");
        jsonWriter.write("\t\t\t\"description\": \"Allows communication between other players for sotetseg maze, special attacks, and more.\"\n");
        jsonWriter.write("\t\t}\n");
        jsonWriter.write("\t]\n");
        jsonWriter.write("}");
        jsonWriter.close();
    }
}