package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.Inventories;
import com.earth2me.essentials.utils.AdventureUtil;
import com.earth2me.essentials.utils.EnumUtil;
import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.VersionUtil;
import net.ess3.api.TranslatableException;
import net.ess3.provider.InventoryViewProvider;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Commandrecipe extends EssentialsCommand {
    private static final Material FIREWORK_ROCKET = EnumUtil.getMaterial("FIREWORK_ROCKET", "FIREWORK");
    private static final Material FIREWORK_STAR = EnumUtil.getMaterial("FIREWORK_STAR", "FIREWORK_CHARGE");
    private static final Material GUNPOWDER = EnumUtil.getMaterial("GUNPOWDER", "SULPHUR");
    private final boolean unsupported;

    public Commandrecipe() {
        super("recipe");
        // On versions at or above 1.12, we need recipe book API
        boolean unsupported = false;
        if (VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_12_0_R01)) {
            try {
                Class.forName("com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent");
            } catch (final ClassNotFoundException e) {
                unsupported = true;
            }
        }
        this.unsupported = unsupported;
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (unsupported) {
            sender.sendTl("unsupportedFeature");
            return;
        }

        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        final ItemStack itemType;

        if (args[0].equalsIgnoreCase("hand")) {
            if (!sender.isPlayer()) {
                throw new TranslatableException("consoleCannotUseCommand");
            }
            
            itemType = Inventories.getItemInHand(sender.getPlayer());
        } else {
            itemType = ess.getItemDb().get(args[0]);
        }

        int recipeNo = 0;

        if (args.length > 1) {
            if (NumberUtil.isInt(args[1])) {
                recipeNo = Integer.parseInt(args[1]) - 1;
            } else {
                throw new TranslatableException("invalidNumber");
            }
        }

        final List<Recipe> recipesOfType = ess.getServer().getRecipesFor(itemType);
        if (recipesOfType.size() < 1) {
            throw new TranslatableException("recipeNone", getMaterialName(sender, itemType));
        }

        if (recipeNo < 0 || recipeNo >= recipesOfType.size()) {
            throw new TranslatableException("recipeBadIndex");
        }

        final Recipe selectedRecipe = recipesOfType.get(recipeNo);
        sender.sendTl("recipe", getMaterialName(sender, itemType), recipeNo + 1, recipesOfType.size());

        if (selectedRecipe instanceof FurnaceRecipe) {
            furnaceRecipe(sender, (FurnaceRecipe) selectedRecipe);
        } else if (selectedRecipe instanceof ShapedRecipe) {
            shapedRecipe(sender, (ShapedRecipe) selectedRecipe, sender.isPlayer());
        } else if (selectedRecipe instanceof ShapelessRecipe) {
            if (recipesOfType.size() == 1 && (itemType.getType() == FIREWORK_ROCKET)) {
                final ShapelessRecipe shapelessRecipe = new ShapelessRecipe(itemType);
                shapelessRecipe.addIngredient(GUNPOWDER);
                shapelessRecipe.addIngredient(Material.PAPER);
                shapelessRecipe.addIngredient(FIREWORK_STAR);
                shapelessRecipe(sender, shapelessRecipe, sender.isPlayer());
            } else {
                shapelessRecipe(sender, (ShapelessRecipe) selectedRecipe, sender.isPlayer());
            }
        }

        if (recipesOfType.size() > 1 && args.length == 1) {
            sender.sendTl("recipeMore", commandLabel, args[0], getMaterialName(sender, itemType));
        }
    }

    public void furnaceRecipe(final CommandSource sender, final FurnaceRecipe recipe) {
        sender.sendTl("recipeFurnace", getMaterialName(sender, recipe.getInput()));
    }

    public void shapedRecipe(final CommandSource sender, final ShapedRecipe recipe, final boolean showWindow) {
        final Map<Character, ItemStack> recipeMap = recipe.getIngredientMap();

        if (showWindow) {
            final User user = ess.getUser(sender.getPlayer());
            user.getBase().closeInventory();
            user.setRecipeSee(true);
            final InventoryView view = user.getBase().openWorkbench(null, true);
            final String[] recipeShape = recipe.getShape();
            final Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();
            for (int j = 0; j < recipeShape.length; j++) {
                for (int k = 0; k < recipeShape[j].length(); k++) {
                    final ItemStack item = ingredientMap.get(recipeShape[j].toCharArray()[k]);
                    if (item == null) {
                        continue;
                    }
                    if (VersionUtil.PRE_FLATTENING && item.getDurability() == Short.MAX_VALUE) {
                        item.setDurability((short) 0);
                    }
                    ess.provider(InventoryViewProvider.class).getTopInventory(view).setItem(j * 3 + k + 1, item);
                }
            }
        } else {
            final HashMap<Material, String> colorMap = new HashMap<>();
            int i = 1;
            for (final Character c : "abcdefghi".toCharArray()) {
                final ItemStack item = recipeMap.get(c);
                if (!colorMap.containsKey(item == null ? null : item.getType())) {
                    colorMap.put(item == null ? null : item.getType(), String.valueOf(i++));
                }
            }
            final Material[][] materials = new Material[3][3];
            for (int j = 0; j < recipe.getShape().length; j++) {
                for (int k = 0; k < recipe.getShape()[j].length(); k++) {
                    final ItemStack item = recipe.getIngredientMap().get(recipe.getShape()[j].toCharArray()[k]);
                    materials[j][k] = item == null ? null : item.getType();
                }
            }
            sender.sendTl("recipeGrid", colorTag(colorMap, materials, 0, 0), colorTag(colorMap, materials, 0, 1), colorTag(colorMap, materials, 0, 2));
            sender.sendTl("recipeGrid", colorTag(colorMap, materials, 1, 0), colorTag(colorMap, materials, 1, 1), colorTag(colorMap, materials, 1, 2));
            sender.sendTl("recipeGrid", colorTag(colorMap, materials, 2, 0), colorTag(colorMap, materials, 2, 1), colorTag(colorMap, materials, 2, 2));

            final StringBuilder s = new StringBuilder();
            for (final Material items : colorMap.keySet().toArray(new Material[0])) {
                s.append(sender.tl("recipeGridItem", colorMap.get(items), getMaterialName(sender, items))).append(" ");
            }
            sender.sendTl("recipeWhere", AdventureUtil.parsed(s.toString()));
        }
    }

    private AdventureUtil.ParsedPlaceholder colorTag(final Map<Material, String> colorMap, final Material[][] materials, final int x, final int y) {
        final char colorChar = colorMap.get(materials[x][y]).charAt(0);
        final NamedTextColor namedTextColor = AdventureUtil.fromChar(colorChar);
        if (namedTextColor == null) {
            throw new IllegalStateException("Illegal amount of materials in recipe");
        }

        return AdventureUtil.parsed("<" + namedTextColor + ">" + colorChar);
    }

    public void shapelessRecipe(final CommandSource sender, final ShapelessRecipe recipe, final boolean showWindow) {
        final List<ItemStack> ingredients = recipe.getIngredientList();
        if (showWindow) {
            final User user = ess.getUser(sender.getPlayer());
            user.getBase().closeInventory();
            user.setRecipeSee(true);
            final InventoryView view = user.getBase().openWorkbench(null, true);
            for (int i = 0; i < ingredients.size(); i++) {
                final ItemStack item = ingredients.get(i);
                if (VersionUtil.PRE_FLATTENING && item.getDurability() == Short.MAX_VALUE) {
                    item.setDurability((short) 0);
                }
                ess.provider(InventoryViewProvider.class).setItem(view, i + 1, item);
            }

        } else {
            final StringBuilder s = new StringBuilder();
            for (int i = 0; i < ingredients.size(); i++) {
                s.append(getMaterialName(sender, ingredients.get(i)));
                if (i != ingredients.size() - 1) {
                    s.append(",");
                }
                s.append(" ");
            }
            sender.sendTl("recipeShapeless", s.toString());
        }
    }

    public String getMaterialName(final CommandSource sender, final ItemStack stack) {
        if (stack == null) {
            return sender.tl("recipeNothing");
        }
        return getMaterialName(sender, stack.getType());
    }

    public String getMaterialName(final CommandSource sender, final Material type) {
        if (type == null) {
            return sender.tl("recipeNothing");
        }
        return type.toString().replace("_", " ").toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getItems();
        } else {
            return Collections.emptyList();
        }
    }
}
