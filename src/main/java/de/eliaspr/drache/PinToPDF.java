package de.eliaspr.drache;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PinToPDF {

    private static int PAGE_HEIGHT = 770;

    public static void createPDF(MessageReceivedEvent event) {
        new Thread(() -> {
            try {
                ArrayList<File> tempFiles = new ArrayList<>();
                TextChannel channel = (TextChannel) event.getChannel();
                channel.sendMessage("PDF wird gerendert und hochgeladen...").queue();
                List<Message> messages = channel.retrievePinnedMessages().complete();

                File tmpFolder = new File("tmp/");
                if (!tmpFolder.exists())
                    tmpFolder.mkdirs();

                PDDocument document = new PDDocument();
                PDDocumentInformation information = document.getDocumentInformation();
                information.setAuthor("Drachenlord");
                information.setTitle("Klausurhilfe für " + channel.getName());
                information.setCreator("eliaspr Drache Bot");

                for (Message msg : messages) {
                    PDPage page = new PDPage();
                    document.addPage(page);
                    PDPageContentStream stream = new PDPageContentStream(document, page);

                    String msgContent = msg.getContentRaw().trim();
                    boolean hasText = false;
                    if (hasText = (msgContent.length() > 0)) {
                        stream.beginText();
                        stream.setFont(PDType1Font.HELVETICA, 14);
                        stream.newLineAtOffset(20, PAGE_HEIGHT - 40);
                        stream.showText(msgContent);
                        stream.endText();
                    }

                    for (Message.Attachment attachment : msg.getAttachments()) {
                        if (attachment.isImage()) {
                            String fileName = UUID.randomUUID().toString() + ".jpg";
                            System.out.println("Downloading " + attachment.getFileName() + " from " + attachment.getProxyUrl() + " as " + fileName);
                            InputStream is = attachment.retrieveInputStream().get();
                            BufferedImage imageData;
                            try {
                                imageData = ImageIO.read(is);
                            } finally {
                                is.close();
                            }
                            File imageTmp = new File(tmpFolder, fileName);
                            tempFiles.add(imageTmp);
                            ImageIO.write(imageData, "JPG", imageTmp);
                            Thread.sleep(200);
                            PDImageXObject pdfImage = PDImageXObject.createFromFileByContent(imageTmp, document);
                            final int width = 500;
                            final int height = (int) (width / ((float) attachment.getWidth() / (float) attachment.getHeight()));
                            final int x = 20;
                            final int y = PAGE_HEIGHT - height - 20 - (hasText ? 50 : 0);
                            stream.drawImage(pdfImage, x, y, width, height);
                        }
                    }

                    stream.close();
                }

                String fileName = UUID.randomUUID() + ".pdf";
                System.out.println("Uploading generated PDF " + fileName);
                File exportFile = new File(tmpFolder, fileName);
                exportFile.deleteOnExit();
                document.save(exportFile);
                document.close();
                event.getChannel().sendFile(exportFile).queue();

                for (File temp : tempFiles) {
                    temp.delete();
                }
            } catch (Exception e) {
                Drache.getInstance().handleException(Thread.currentThread(), e, true, event.getChannel());
            }
        }, "Create-PDF").start();
    }

}
