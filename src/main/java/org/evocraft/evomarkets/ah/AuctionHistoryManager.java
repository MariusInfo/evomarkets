package org.evocraft.evomarkets.ah;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class AuctionHistoryManager {
    private static AuctionHistoryManager INSTANCE;
    private final File saveFile;
    private final Gson gson;
    // Seller UUID -> Lista de tranzactii
    private Map<UUID, List<Transaction>> history = new HashMap<>();

    public AuctionHistoryManager() {
        this.saveFile = FMLPaths.CONFIGDIR.get().resolve("evo_markets_auction_history.json").toFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public static AuctionHistoryManager get() {
        if (INSTANCE == null) INSTANCE = new AuctionHistoryManager();
        return INSTANCE;
    }

    // Adauga o tranzactie noua (Apelat cand cineva cumpara)
    public void addTransaction(UUID seller, String buyerName, String itemName, double price, double tax) {
        Transaction t = new Transaction();
        t.buyerName = buyerName;
        t.itemName = itemName;
        t.price = price;
        t.tax = tax;
        t.date = new SimpleDateFormat("dd/MM HH:mm").format(new Date());

        history.computeIfAbsent(seller, k -> new ArrayList<>()).add(0, t); // Adaugam la inceputul listei

        // Pastram doar ultimele 20 tranzactii per player ca sa nu umplem fisierul
        List<Transaction> userHistory = history.get(seller);
        if (userHistory.size() > 20) {
            userHistory.remove(userHistory.size() - 1);
        }

        save();
    }

    public List<Transaction> getHistory(UUID seller) {
        return history.getOrDefault(seller, new ArrayList<>());
    }

    public void save() {
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(history, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        if (!saveFile.exists()) return;
        try (Reader reader = new FileReader(saveFile)) {
            history = gson.fromJson(reader, new TypeToken<Map<UUID, List<Transaction>>>(){}.getType());
            if (history == null) history = new HashMap<>();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static class Transaction {
        public String buyerName;
        public String itemName;
        public String date;
        public double price;
        public double tax;
    }
}