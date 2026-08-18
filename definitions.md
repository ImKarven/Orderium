> [!NOTE]
> This file does not fully explain how this plugin works.
> <br>
> It is not fully completed. You may use your common sense to understand it.
> <br>
> It is created as a part of `modrinth/disclosures/telemetry.md`.

# Definitions

## Orderium
"Orderium" refers to this project.

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

a 64-bit integer (as explained in 32-bit integer) that is the sum of the config option `expires-after` and the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.

### Creation
An Orderium Order must be created by a Player.
<br>
The Player who creates this Orderium Order ("Owner Player") must provide:
- an Orderium Item as `item`;
- a decimal as `moneyPer` that must be equal or higher than config option `minimum-price`;
- a positive integer as `amount`.

Additionally, `moneyPer * amount` must be equal or lower than the Player's Balance.
<br>
If all the requirements are met, the Player's Balance is deducted by `moneyPer * amount` and an Orderium Order is created.

### Collection
The Owner Player of an Orderium Order can collect copies of `item`.
<br>
The Owner Player must provide:
- a positive integer that must be equal or lower than `inStorage` and config option `max-collect` (`collectAmount`).
<br>
If all the requirements are met, the Owner Player is given `collectAmount` copies of the Orderium Order's Orderium Item.

## Orderium Item
Orderium Item is an [Item](https://minecraft.wiki/w/Item) that does not have Stacking ("Amount-less Item") in [the Creative inventory](https://minecraft.wiki/w/Creative_inventory) or the list of Custom Items that is not present in the Blacklist at the time of choosing the Orderium Item when creating an Orderium Order.
<br>

## Custom Item
a Custom Item is an Amount-less Item.

## Blacklist
a list of Amount-less Items.