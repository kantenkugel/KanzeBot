package com.kantenkugel.discordbot.listener;

import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.hooks.EventListener;

public class InviteListener implements EventListener {
    @Override
    public void onEvent(Event e) {
        /*
        if(e.getClass() == InviteReceivedEvent.class) {
            InviteReceivedEvent event = (InviteReceivedEvent) e;
            if(BlackList.contains(event.getAuthor())) {
                return;
            }
            MessageChannel channel = event.isPrivate() ? event.getJDA().getPrivateChannelById(event.getMessage().getChannelId()) :
                    event.getJDA().getTextChannelById(event.getMessage().getChannelId());
            if((event.isPrivate() || event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo()))) {
                if(event.getJDA().getGuildById(event.getInvite().getGuildId()) != null) {
                    try {
                        channel.sendMessage("Already in that Server!");
                    } catch (RuntimeException ignored) {} //no write perms or blocked pm
                    return;
                }
                Statics.LOG.info("Joining Guild " + event.getInvite().getGuildName() + " via invite of " + event.getAuthor().getUsername());
                InviteUtil.join(event.getInvite(), event.getJDA(), guild -> {
                    String text = "Joined Guild " + guild.getName() + "! Server owner should probably configure me via the config command\nDefault command-prefix is: "
                            + ServerConfig.DEFAULT_PREFIX + "\nThe owner can reset it by calling -kbreset";
                    try {
                        channel.sendMessage(text);
                    } catch(RuntimeException ignored) {} //no write perms or blocked pm
                    text = "Joined Guild via invite of " + event.getAuthor().getUsername() + " (ID: " + event.getAuthor().getId() +
                            ")! Server owner should probably configure me via the config command\n" +
                            "Default command-prefix is: " + ServerConfig.DEFAULT_PREFIX + "\nThe owner can reset it by calling -kbreset";
                    if(guild.getPublicChannel().checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE)) {
                        guild.getPublicChannel().sendMessageAsync(text, null);
                    } else {
                        Optional<TextChannel> first = guild.getTextChannels().parallelStream().filter(tc -> tc.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE))
                                .sorted((c1, c2) -> Integer.compare(c1.getPosition(), c2.getPosition())).findFirst();
                        if(first.isPresent()) {
                            first.get().sendMessage(text);
                        }
                    }
                });
            }
        }
        */
    }
}
