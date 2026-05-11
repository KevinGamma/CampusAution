package com.campus.auction.service.impl;

import com.campus.auction.dto.SeedResult;
import com.campus.auction.entity.Auction;
import com.campus.auction.entity.User;
import com.campus.auction.enums.AuctionStatus;
import com.campus.auction.enums.SaleType;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.AuctionService;
import com.campus.auction.service.DataSeederService;
import com.campus.auction.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DataSeederServiceImpl implements DataSeederService {

    private final UserService    userService;
    private final AuctionService auctionService;

    private static final String[] TITLES = {
        "Used RTX 3060 Ti Graphics Card",
        "MacBook Pro 13\" M1 2021",
        "iPad Air 4th Gen 64GB",
        "Keychron K2 Mechanical Keyboard",
        "Sony WH-1000XM4 Headphones",
        "Dell UltraSharp 27\" Monitor",
        "Logitech MX Master 3 Mouse",
        "Nintendo Switch OLED White",
        "Apple AirPods Pro 2nd Gen",
        "Samsung Galaxy S22 128GB",
        "Advanced Mathematics Textbook 4th Ed",
        "Calculus: Early Transcendentals",
        "Introduction to Algorithms (CLRS)",
        "Physics for Scientists & Engineers",
        "Organic Chemistry 8th Edition",
        "Java: The Complete Reference",
        "Data Structures Handwritten Notes",
        "Linear Algebra Study Pack",
        "Principles of Economics",
        "English Literature Anthology",
        "Campus Road Bicycle 21-speed",
        "Folding Electric Scooter",
        "Yoga Mat + Foam Blocks Set",
        "20kg Adjustable Dumbbell Set",
        "Head Tennis Racket Pro",
        "Official Size Basketball Spalding",
        "Nike Running Shoes Size 42",
        "60L Camping Backpack",
        "Whey Protein Powder 5lbs",
        "Speed Jump Rope Pro",
        "Nespresso Coffee Maker",
        "LED Desk Lamp Dimmable",
        "Mini Fridge 15L White",
        "3-cup Rice Cooker Philips",
        "Foldable Study Desk",
        "JBL Flip 6 Bluetooth Speaker",
        "7-in-1 USB-C Hub",
        "20000mAh Portable Charger",
        "Dyson V8 Handheld Vacuum",
        "Instant Pot Duo 6qt",
    };

    private static final String[] DESCRIPTIONS = {
        "In excellent condition, barely used. Perfect for students on a budget.",
        "Lightly used for one semester, no scratches or damage. All accessories included.",
        "Good working condition with minor surface wear. Priced to sell fast.",
        "Purchased last semester, selling because I upgraded. Great value.",
        "Original packaging and receipt included. Works flawlessly.",
        "Some cosmetic wear on the body but fully functional and reliable.",
        "Selling as I am graduating. Pick up on campus, no shipping needed.",
        "Like new — only used for one semester during study sessions.",
        "Well maintained with all functions working perfectly. Must-have item.",
        "Selling at half the retail price. Slight discolouration but otherwise excellent.",
    };

    private static final String[] CATEGORIES = {
        "Electronics", "Books", "Daily Use", "Sports", "Others"
    };

    private static final String[] DIRECT_TITLES = {
        "New Notebook — Fixed Price",
        "Campus Meal Card (¥100 Face Value)",
        "Used Bicycle — Fixed Price",
        "Dorm Mini Fridge — Buy Now",
        "Second-Hand Scientific Calculator",
        "Brand New Earphones (Sealed Box)",
        "Campus Bus Pass — 1 Month",
        "Portable Whiteboard Marker Set",
        "Used English Dictionary (Oxford)",
        "Folding Umbrella — Waterproof",
        "Laundry Bag + Detergent Bundle",
        "USB Desk Fan — Buy Now",
        "Graph Paper Notebook (Pack of 3)",
        "Badminton Racket Set (2 pcs)",
        "Stainless Steel Water Bottle 600ml",
        "Phone Stand — Adjustable",
        "Reusable Shopping Bag (5-Pack)",
        "Wireless Mouse — Plug & Play",
        "LED Reading Lamp — USB Powered",
        "Campus Canteen Combo Voucher",
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeedResult seed(int userCount, int auctionsPerUser, int directPercent, int directSaleCount) {
        Random rng = new Random();
        // Short timestamp suffix keeps usernames unique across multiple seed runs
        String suffix = String.valueOf(System.currentTimeMillis() % 1_000_000);

        // ── 1. Build and batch-insert users ───────────────────────────────────
        List<User> users = new ArrayList<>(userCount);
        for (int i = 1; i <= userCount; i++) {
            User u = new User();
            u.setUsername("student_" + suffix + "_" + i);
            u.setPassword("password123");
            u.setBalance(BigDecimal.valueOf(100 + rng.nextInt(9_900)));
            u.setRole(UserRole.STUDENT);
            users.add(u);
        }
        userService.saveBatch(users);
        // After saveBatch, MyBatis-Plus fills each entity's auto-generated id

        // ── 2. Build per-user auction/direct mix ──────────────────────────────
        List<Auction> auctions = new ArrayList<>(userCount * auctionsPerUser + directSaleCount);
        for (User u : users) {
            for (int j = 0; j < auctionsPerUser; j++) {
                BigDecimal price = BigDecimal.valueOf(10 + rng.nextInt(9_990));

                boolean isDirect = rng.nextInt(100) < directPercent;
                SaleType saleType = isDirect ? SaleType.DIRECT : SaleType.AUCTION;

                Auction a = new Auction();
                a.setTitle(TITLES[rng.nextInt(TITLES.length)]);
                a.setDescription(DESCRIPTIONS[rng.nextInt(DESCRIPTIONS.length)]);
                a.setStartPrice(price);
                a.setCurrentPrice(price);
                // Auctions expire in 1–30 days; direct-sale items use a 10-year sentinel
                a.setEndTime(isDirect
                        ? LocalDateTime.now().plusYears(10)
                        : LocalDateTime.now().plusDays(1 + rng.nextInt(30)));
                a.setStatus(AuctionStatus.ACTIVE);
                a.setCreatorId(u.getId());
                a.setCategory(CATEGORIES[rng.nextInt(CATEGORIES.length)]);
                a.setQuantity(1 + rng.nextInt(10));
                a.setSaleType(saleType);
                auctions.add(a);
            }
        }

        // ── 3. Build standalone fixed-price direct-sale items ──────────────────
        // Distributed round-robin across the seeded users so every item has a valid seller.
        for (int k = 0; k < directSaleCount; k++) {
            User seller = users.get(k % users.size());
            BigDecimal price = BigDecimal.valueOf(5 + rng.nextInt(995));

            Auction a = new Auction();
            a.setTitle(DIRECT_TITLES[rng.nextInt(DIRECT_TITLES.length)]);
            a.setDescription(DESCRIPTIONS[rng.nextInt(DESCRIPTIONS.length)]);
            a.setStartPrice(price);
            a.setCurrentPrice(price);
            a.setEndTime(LocalDateTime.now().plusYears(10));
            a.setStatus(AuctionStatus.ACTIVE);
            a.setCreatorId(seller.getId());
            a.setCategory(CATEGORIES[rng.nextInt(CATEGORIES.length)]);
            a.setQuantity(1 + rng.nextInt(5));
            a.setSaleType(SaleType.DIRECT);
            auctions.add(a);
        }

        auctionService.saveBatch(auctions);

        return new SeedResult(users.size(), auctions.size());
    }
}
