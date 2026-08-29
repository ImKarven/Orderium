> [!NOTE]
> This file does not fully explain how this plugin works.
> <br>
> It is not fully completed. You may use your common sense to understand it.
> <br>
> It is created as a part of `modrinth/disclosures/telemetry.md`.

# Definitions

## Disambiguation
There may be multiple definitions for one phrase/word.
<br>
You may understand what the text refers to based on the context.
<br>
The text may disambiguate such phrases/words, but this is not guaranteed.

## Orderium
"Orderium" refers to this project.
<br>
Orderium may be represented as a Paper plugin, a Github repository or a Modrinth project.

## Server
"Server", "the Server", "your Server" is the [Java virtual machine](https://en.wikipedia.org/wiki/Java_virtual_machine) that Orderium (as a Paper plugin) runs in.

## Config Option
Config Option is an option in Orderium's configuration files.

### General
General Config Options are in `config.yml` file, in Orderium's Data Folder.

### GUI
Each GUI in Orderium has its configuration file. These files are located in the `gui` directory in Orderium's Data Folder.

#### Enchant GUI
Enchant GUI Config Options are in `enchant.yml` file.

## Orderium Order

### Values
An Orderium Order has these values:
#### `id`
a unique identifier for this Orderium Order in your Server.

#### `owner`
the [Player UUID](https://minecraft.wiki/w/UUID#Player_UUID) of the Player (as explained in Player UUID) who created this Orderium Order.

#### `item`
An Orderium Item chosen by the owner of this Orderium Order.

#### `moneyPer`
a [double](https://en.wikipedia.org/wiki/Double-precision_floating-point_format).

#### `amount`
an integer that has a minimum value of `-2³¹` and a maximum value of `2³¹-1` ("32-bit integer").

#### `delivered`
a 32-bit integer.

#### `inStorage`
a 32-bit integer.

#### `expiresAt`

a 64-bit integer (as explained in 32-bit integer) that is the sum of the General Config Option `expires-after` and the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.

### Creation
An Orderium Order must be created by a Player.
<br>
The Player who creates this Orderium Order ("Owner Player") must provide:
- an Orderium Item as `item`;
- a decimal as `moneyPer` that must be equal or higher than General Config Option `minimum-price`;
- a positive integer as `amount`.

Additionally, `moneyPer * amount` must be equal or lower than the Player's Balance.
<br>
If all the requirements are met, the Player's Balance is deducted by `moneyPer * amount` and an Orderium Order is created.

### Collection
The Owner Player of an Orderium Order can collect copies of `item`.
<br>
The Owner Player must provide:
- a positive integer that must be equal or lower than `inStorage` and General Config Option `max-collect` (`collectAmount`).
<br>
If all the requirements are met, the Owner Player is given `collectAmount` copies of the Orderium Order's Orderium Item.

## Orderium Item
Orderium Item is an [Item](https://minecraft.wiki/w/Item) that does not have Stacking ("Amount-less Item") in [the Creative inventory](https://minecraft.wiki/w/Creative_inventory) or the list of Custom Items that is not present in the Blacklist at the time of choosing the Orderium Item when creating an Orderium Order.
<br>

## Custom Item
a Custom Item is an Amount-less Item.

## Blacklist
a list of Amount-less Items.

## Orderium API
Orderium provides an unfinished, experimental API. Files related to this API is in the `src/main/java/me/karven/orderium/api` directory of Orderium (as a Github repository), recursively.

### Usage
The Server is considered using Orderium API if it listens to any Bukkit Events that is a part of Orderium API.

## Experimental Features
Some features in Orderium are "experimental".

### Custom Item
The Server is considered using Custom Item (as an Experimental Feature) if the list of Custom Items is not empty.

### Enchant GUI
The Server is considered using Enchant GUI (as an Experimental Feature) if Enchant GUI Config Option `enabled` is set to `true`.
