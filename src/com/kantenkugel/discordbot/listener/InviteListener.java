package com.kantenkugel.discordbot.listener;

import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.config.BlackList;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.InviteReceivedEvent;
import net.dv8tion.jda.hooks.EventListener;

public class InviteListener implements EventListener {
    @Override
    public void onEvent(Event e) {
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
                if(Statics.OAUTH_ID.trim().length() == 0) {
                    channel.sendMessageAsync("I am currently not configured to accept invites!", null);
                } else {
                    channel.sendMessageAsync("I can no longer be invited via invite-links! " +
                            "Please use following link to invite me to your server (manage_server permission required): " +
                            "https://discordapp.com/oauth2/authorize?&client_id=" + Statics.OAUTH_ID + "&scope=bot", null);
                }
            }
        }
    }
}
