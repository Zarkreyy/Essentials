package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.Inventories;
import com.earth2me.essentials.utils.EnumUtil;
import com.earth2me.essentials.utils.FormatUtil;
import com.earth2me.essentials.utils.VersionUtil;
import com.google.common.collect.Lists;
import net.ess3.api.TranslatableException;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;

import java.util.Collections;
import java.util.List;

public class Commandbook extends EssentialsCommand {

    private static final Material WRITABLE_BOOK = EnumUtil.getMaterial("WRITABLE_BOOK", "BOOK_AND_QUILL");

    public Commandbook() {
        super("book");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack item = Inventories.getItemInMainHand(user.getBase());
        final String player = user.getName();
        if (item.getType() == Material.WRITTEN_BOOK) {
            final BookMeta bmeta = (BookMeta) item.getItemMeta();

            if (args.length > 1 && args[0].equalsIgnoreCase("author")) {
                if (user.isAuthorized("essentials.book.author") && (isAuthor(bmeta, player) || user.isAuthorized("essentials.book.others"))) {
                    final String newAuthor = FormatUtil.formatString(user, "essentials.book.author", getFinalArg(args, 1)).trim();
                    bmeta.setAuthor(newAuthor);
                    item.setItemMeta(bmeta);
                    user.sendTl("bookAuthorSet", newAuthor);
                } else {
                    throw new TranslatableException("denyChangeAuthor");
                }
            } else if (args.length > 1 && args[0].equalsIgnoreCase("title")) {
                if (user.isAuthorized("essentials.book.title") && (isAuthor(bmeta, player) || user.isAuthorized("essentials.book.others"))) {
                    final String newTitle = FormatUtil.formatString(user, "essentials.book.title", getFinalArg(args, 1)).trim();
                    bmeta.setTitle(newTitle);
                    item.setItemMeta(bmeta);
                    user.sendTl("bookTitleSet", newTitle);
                } else {
                    throw new TranslatableException("denyChangeTitle");
                }
            } else {
                if (isAuthor(bmeta, player) || user.isAuthorized("essentials.book.others")) {
                    final ItemStack newItem = new ItemStack(WRITABLE_BOOK, item.getAmount());
                    if (VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_20_6_R01)) {
                        final WritableBookMeta wbmeta = (WritableBookMeta) newItem.getItemMeta();
                        //noinspection DataFlowIssue
                        wbmeta.setPages(bmeta.getPages());
                        newItem.setItemMeta(wbmeta);
                    } else {
                        newItem.setItemMeta(bmeta);
                    }
                    Inventories.setItemInMainHand(user.getBase(), newItem);
                    user.sendTl("editBookContents");
                } else {
                    throw new TranslatableException("denyBookEdit");
                }
            }
        } else if (item.getType() == WRITABLE_BOOK) {
            final BookMeta bmeta = (BookMeta) item.getItemMeta();
            if (!user.isAuthorized("essentials.book.author")) {
                bmeta.setAuthor(player);
            }
            final ItemStack newItem = new ItemStack(Material.WRITTEN_BOOK, item.getAmount());
            if (VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_20_6_R01)) {
                final BookMeta real = (BookMeta) newItem.getItemMeta();
                real.setAuthor(bmeta.getAuthor());
                real.setTitle(bmeta.getTitle());
                real.setPages(bmeta.getPages());
                newItem.setItemMeta(real);
            } else {
                newItem.setItemMeta(bmeta);
            }
            Inventories.setItemInMainHand(user.getBase(), newItem);
            user.sendTl("bookLocked");
        } else {
            throw new TranslatableException("holdBook");
        }
    }

    private boolean isAuthor(final BookMeta bmeta, final String player) {
        final String author = bmeta.getAuthor();
        return author != null && author.equalsIgnoreCase(player);
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        // Right now, we aren't testing what's held in the player's hand - we could, but it's not necessarily worth it
        if (args.length == 1) {
            final List<String> options = Lists.newArrayList("sign", "unsign"); // sign and unsign aren't real, but work
            if (user.isAuthorized("essentials.book.author")) {
                options.add("author");
            }
            if (user.isAuthorized("essentials.book.title")) {
                options.add("title");
            }
            return options;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("author") && user.isAuthorized("essentials.book.author")) {
            final List<String> options = getPlayers(server, user);
            options.add("Herobrine"); // #EasterEgg
            return options;
        } else {
            return Collections.emptyList();
        }
    }
}
