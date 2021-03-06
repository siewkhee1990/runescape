package com.openrsc.server.model.entity.player;

import com.openrsc.server.database.impl.mysql.queries.logging.DeathLog;
import com.openrsc.server.model.container.ContainerListener;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.container.ItemContainer;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.net.rsc.ActionSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Duel implements ContainerListener {
	public static Logger LOGGER = LogManager.getLogger();
	private Player player;
	private Player duelRecipient;

	private boolean[] duelOptions = new boolean[4];

	private boolean duelAccepted;

	private boolean duelConfirmAccepted;

	private boolean duelActive;

	private ItemContainer duelOffer;

	public Duel(Player player) {
		this.player = player;
		this.duelOffer = new ItemContainer(player, 12, false);
	}


	public Player getPlayer() {
		return player;
	}


	public void setPlayer(Player player) {
		this.player = player;
	}


	public Player getDuelRecipient() {
		return duelRecipient;
	}


	public void setDuelRecipient(Player duelRecipient) {
		this.duelRecipient = duelRecipient;
	}


	public boolean isDuelAccepted() {
		return duelAccepted;
	}


	public void setDuelAccepted(boolean duelAccepted) {
		this.duelAccepted = duelAccepted;
	}


	public boolean isDuelConfirmAccepted() {
		return duelConfirmAccepted;
	}


	public void setDuelConfirmAccepted(boolean duelConfirmAccepted) {
		this.duelConfirmAccepted = duelConfirmAccepted;
	}


	public boolean isDuelActive() {
		return duelActive;
	}


	public void setDuelActive(boolean duelActive) {
		this.duelActive = duelActive;
	}


	public ItemContainer getDuelOffer() {
		return duelOffer;
	}


	public void setDuelOffer(ItemContainer duelOffer) {
		this.duelOffer = duelOffer;
	}


	@Override
	public void fireItemChanged(int slot) {
		// TODO Auto-generated method stub

	}

	public void clearDuelOptions() {
		for (int i = 0; i < 4; i++) {
			duelOptions[i] = false;
		}
	}

	public boolean getDuelSetting(int i) {
		return duelOptions[i];
	}

	public void setDuelSetting(int i, boolean b) {
		duelOptions[i] = b;
	}

	@Override
	public void fireItemsChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void fireContainerFull() {
		// TODO Auto-generated method stub

	}

	public void resetAll() {
		Player duelOpponent = getDuelRecipient();

		if (duelOpponent != null) {
			setDuelRecipient(null);
			duelOpponent.getDuel().resetAll();
		}

		if (isDuelActive()) {
			ActionSender.sendDuelWindowClose(player);
		}

		setDuelActive(false);
		setDuelAccepted(false);
		setDuelConfirmAccepted(false);

		resetDuelOffer();
		clearDuelOptions();
	}


	public void resetDuelOffer() {
		duelOffer.clear();
	}


	public void addToDuelOffer(Item tItem) {
		duelOffer.add(tItem);
	}


	public void dropOnDeath() {
		DeathLog log = new DeathLog(player, duelRecipient, true);
		Player duelOpponent = getDuelRecipient();
		synchronized(getDuelOffer().getItems()) {
			for (Item item : getDuelOffer().getItems()) {
				Item affectedItem = player.getCarriedItems().getInventory().get(item);
				if (affectedItem == null) {
					if (player.getConfig().WANT_EQUIPMENT_TAB && item.getAmount() == 1 && player.getCarriedItems().getEquipment().hasEquipped(item.getCatalogId())) {
						player.updateWornItems(item.getDef(player.getWorld()).getWieldPosition(),
							player.getSettings().getAppearance().getSprite(item.getDef(player.getWorld()).getWieldPosition()),
							item.getDef(player.getWorld()).getWearableId(), false);
						player.getCarriedItems().getEquipment().remove(item, item.getAmount());
						log.addDroppedItem(item);
						player.getWorld().registerItem(new GroundItem(duelOpponent.getWorld(), item.getCatalogId(), player.getX(), player.getY(), item.getAmount(), duelOpponent));
					}
					LOGGER.info("Missing staked item [" + item.getCatalogId() + ", " + item.getAmount()
						+ "] from = " + player.getUsername() + "; to = " + duelRecipient.getUsername() + ";");
				} else {
					player.getCarriedItems().remove(new Item(item.getCatalogId(), item.getAmount(), item.getNoted(), affectedItem.getItemId()));
					log.addDroppedItem(item);
					player.getWorld().registerItem(new GroundItem(duelOpponent.getWorld(), item.getCatalogId(), player.getX(), player.getY(), item.getAmount(), duelOpponent, item.getNoted()));
				}
			}
		}
		log.build();
		player.getWorld().getServer().getGameLogger().addQuery(log);

		if (player != null && duelOpponent != null) {
			player.save();
			duelOpponent.save();
		}
	}
}
