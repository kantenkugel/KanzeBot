/*
 * Copyright 2016 Michael Ritter (Kantenkugel)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kantenkugel.discordbot.commands.sections;

import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

public class MiscCommands implements CommandSection {
    @Override
    public void register(Map<String, Command> registry, JDA api) {
        registry.put("feedback", new CommandWrapper("Used to give Feedback about KanzeBot", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length == 2) {
                Statics.botOwner.getPrivateChannel().sendMessageAsync(new MessageBuilder().appendString("**[Feedback]** ")
                                .appendString(e.getAuthor().getUsername()).appendString(" (").appendString(e.getAuthor().getId())
                                .appendString(")\n").appendString(args[1]).build()
                        , m -> reply(e, cfg, "Thanks! Your feedback has been saved!"));
            } else {
                reply(e, cfg, "Usage: `feedback FEEDBACK`");
            }
        }));

        registry.put("mentioned", new CommandWrapper("Looks for the last message in this Channel where you got mentioned.", (e, cfg) -> {
            MessageHistory messageHistory = new MessageHistory(e.getTextChannel());
            User user = e.getMessage().getMentionedUsers().size() > 0 ? e.getMessage().getMentionedUsers().get(0) : e.getAuthor();
            for(int i = 0; i < 5; i++) {
                List<Message> msgs = messageHistory.retrieve();
                if(msgs == null) {
                    reply(e, cfg, "You have never been mentioned in this channel before!");
                    return;
                }
                for(Message msg : msgs) {
                    if((msg.getMentionedUsers().contains(user) || msg.mentionsEveryone()) && !msg.getContent().startsWith(cfg.getPrefix())) {
                        reply(e, cfg, "Last mention of " + user.getUsername() + " was at " + msg.getTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + " from "
                                + (msg.getAuthor() == null ? "-" : msg.getAuthor().getUsername()) + "\nContent: " + msg.getContent());
                        return;
                    }
                }
                if(msgs.size() < 100) {
                    reply(e, cfg, "You have never been mentioned in this channel before!");
                    return;
                }
            }
            reply(e, cfg, "Last mention is older than 500 messages!");
        }).acceptPrivate(false));

        registry.put("rip", new CommandWrapper("Rest in Pieces", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length == 1) {
                reply(e, cfg, "https://cdn.discordapp.com/attachments/116705171312082950/120787560988540929/rip2.png", false);
                return;
            }
            if(!e.isPrivate() && !e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES)) {
                reply(e, cfg, "I cannot upload files here!");
                return;
            }
            String text;
            BufferedImage avatar = null;
            if(e.getMessage().getMentionedUsers().size() > 0) {
                text = e.getMessage().getMentionedUsers().get(0).getUsername();
                try {
                    InputStream stream = MiscUtil.getDataStream(e.getMessage().getMentionedUsers().get(0).getAvatarUrl());
                    if(stream != null) {
                        avatar = ImageIO.read(stream);
                    }
                } catch(IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                text = args[1];
            }
            try {
                //left edge at 30, right one at 210 => 180 width
                //y = 200
                BufferedImage read = ImageIO.read(MiscCommands.class.getClassLoader().getResourceAsStream("rip.png"));
                BufferedImage image = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.drawImage(read, 0, 0, null);
                g.setColor(Color.black);
                Font font = new Font("Arial Black", Font.BOLD, 50);
                FontMetrics m = g.getFontMetrics(font);
                while(m.stringWidth(text) > 180) {
                    font = new Font("Arial Black", Font.BOLD, font.getSize() - 1);
                    m = g.getFontMetrics(font);
                }
                g.setFont(font);
                g.drawString(text, 30 + (180 - m.stringWidth(text)) / 2, 190);
                if(avatar != null) {
                    g.drawImage(avatar, 90, 200, 60, 60, null);
                }
                g.dispose();
                File tmpFile = new File("rip_" + e.getResponseNumber() + ".png");
                ImageIO.write(image, "png", tmpFile);
                e.getChannel().sendFileAsync(tmpFile, null, mess -> tmpFile.delete());
            } catch(IOException e1) {
                reply(e, cfg, "I made a Boo Boo!");
            }
        }));
    }
}
