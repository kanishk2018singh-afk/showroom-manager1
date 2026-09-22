package com.example.data

object InitialData {

    val defaultCompanies = listOf(
        Company(id = 1, name = "Hindware", description = "Plumbing fixtures, sanitaryware & CP fittings", isDefault = true),
        Company(id = 2, name = "Jaquar", description = "Premium bathroom fittings, showers & wellness", isDefault = false),
        Company(id = 3, name = "Cera", description = "Contemporary sanitaryware, tiles & faucets", isDefault = false),
        Company(id = 4, name = "Kohler", description = "Luxury bathroom & kitchen fittings", isDefault = false),
        Company(id = 5, name = "Parryware", description = "Affordable and durable bathroom solutions", isDefault = false),
        Company(id = 6, name = "Local Supplier", description = "General hardware, adhesives and accessories", isDefault = false)
    )

    val defaultCategories = listOf(
        CategoryEntity(
            id = 1,
            name = "CP",
            subcategories = "Tap, Mixer, Shower, Faucet, Diverter, Spout"
        ),
        CategoryEntity(
            id = 2,
            name = "Sanitary",
            subcategories = "WC, Basin, Urinal, Cistern, Seat Cover"
        ),
        CategoryEntity(
            id = 3,
            name = "Hardware",
            subcategories = "Handle, Lock, Hinge, Tower Bolt, Drawer Channel"
        ),
        CategoryEntity(
            id = 4,
            name = "Accessories",
            subcategories = "Towel Rail, Soap Dish, Robe Hook, Paper Holder, Corner Shelf"
        ),
        CategoryEntity(
            id = 5,
            name = "Bath Fittings",
            subcategories = "Vanity Cabinet, LED Mirror, Glass Partition, Floor Drain"
        ),
        CategoryEntity(
            id = 6,
            name = "Kitchen",
            subcategories = "Kitchen Sink, Sink Mixer, Pull-out Faucet, Waste Coupling"
        ),
        CategoryEntity(
            id = 7,
            name = "Other",
            subcategories = "Tile Adhesive, Epoxy Grout, Cleaner, Teflon Tape"
        )
    )

    fun getDefaultProducts(): List<Product> = listOf(
        // Hindware Products (Company 1)
        Product(
            id = 1,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-1234",
            name = "Wall Mixer 3-in-1 with Provision for Overhead Shower",
            brand = "Hindware",
            category = "CP",
            subcategory = "Mixer",
            imageUri = "icon:mixer",
            mrp = 10000.0,
            discountPercent = 35.0,
            gstPercent = 18.0,
            purchasePrice = 5000.0,
            stockQuantity = 24,
            notes = "Contessa series chrome finish. Best seller."
        ),
        Product(
            id = 2,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-02",
            name = "Italian Collection Oval Table Top Basin",
            brand = "Hindware",
            category = "Sanitary",
            subcategory = "Basin",
            imageUri = "icon:basin",
            mrp = 8000.0,
            discountPercent = 20.0,
            gstPercent = 18.0,
            purchasePrice = 4200.0,
            stockQuantity = 15,
            notes = "Glossy white glazed ceramic table top."
        ),
        Product(
            id = 3,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-1088",
            name = "Single Lever Concealed Diverter High Flow",
            brand = "Hindware",
            category = "CP",
            subcategory = "Diverter",
            imageUri = "icon:tap",
            mrp = 6400.0,
            discountPercent = 30.0,
            gstPercent = 18.0,
            purchasePrice = 3200.0,
            stockQuantity = 18,
            notes = "Cartridge 40mm, brass body."
        ),
        Product(
            id = 4,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-3021",
            name = "One Piece Rimless Water Closet (S-Trap 300mm)",
            brand = "Hindware",
            category = "Sanitary",
            subcategory = "WC",
            imageUri = "icon:wc",
            mrp = 14500.0,
            discountPercent = 25.0,
            gstPercent = 18.0,
            purchasePrice = 7800.0,
            stockQuantity = 10,
            notes = "Soft close hydraulic seat cover included."
        ),
        Product(
            id = 5,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-550",
            name = "Rain Shower Head 200x200mm SS304 with Arm",
            brand = "Hindware",
            category = "CP",
            subcategory = "Shower",
            imageUri = "icon:shower",
            mrp = 3200.0,
            discountPercent = 35.0,
            gstPercent = 18.0,
            purchasePrice = 1400.0,
            stockQuantity = 32,
            notes = "Ultra thin mirror finish with silicon nozzles."
        ),
        Product(
            id = 6,
            companyId = 1,
            companyName = "Hindware",
            code = "HW-ACC-07",
            name = "Stainless Steel Double Towel Rail 600mm",
            brand = "Hindware",
            category = "Accessories",
            subcategory = "Towel Rail",
            imageUri = "icon:accessories",
            mrp = 2200.0,
            discountPercent = 40.0,
            gstPercent = 18.0,
            purchasePrice = 900.0,
            stockQuantity = 45,
            notes = "Heavy gauge SS 304 with concealed screws."
        ),

        // Jaquar Products (Company 2)
        Product(
            id = 7,
            companyId = 2,
            companyName = "Jaquar",
            code = "JQ-FLR-5115",
            name = "Florentine Single Lever Basin Mixer without Popup",
            brand = "Jaquar",
            category = "CP",
            subcategory = "Mixer",
            imageUri = "icon:tap",
            mrp = 5500.0,
            discountPercent = 20.0,
            gstPercent = 18.0,
            purchasePrice = 3300.0,
            stockQuantity = 20,
            notes = "Jaquar classic Florentine series."
        ),
        Product(
            id = 8,
            companyId = 2,
            companyName = "Jaquar",
            code = "JQ-OP-219",
            name = "Opal Prime Wall Hung Basin with Half Pedestal",
            brand = "Jaquar",
            category = "Sanitary",
            subcategory = "Basin",
            imageUri = "icon:basin",
            mrp = 7200.0,
            discountPercent = 15.0,
            gstPercent = 18.0,
            purchasePrice = 4500.0,
            stockQuantity = 12,
            notes = "Premium sanitaryware white ceramic."
        ),
        Product(
            id = 9,
            companyId = 2,
            companyName = "Jaquar",
            code = "JQ-MAZE-SH",
            name = "Maze Multi-Flow Hand Shower Set with 1.5m Hose",
            brand = "Jaquar",
            category = "CP",
            subcategory = "Shower",
            imageUri = "icon:shower",
            mrp = 2800.0,
            discountPercent = 22.0,
            gstPercent = 18.0,
            purchasePrice = 1600.0,
            stockQuantity = 28,
            notes = "3 spray settings: Rain, Massage, Mist."
        ),

        // Cera Products (Company 3)
        Product(
            id = 10,
            companyId = 3,
            companyName = "Cera",
            code = "CR-CAL-104",
            name = "Calibre Wall Hung Toilet with UF Soft Close Seat",
            brand = "Cera",
            category = "Sanitary",
            subcategory = "WC",
            imageUri = "icon:wc",
            mrp = 9800.0,
            discountPercent = 25.0,
            gstPercent = 18.0,
            purchasePrice = 5200.0,
            stockQuantity = 14,
            notes = "Rimless bowl design with anti-bacterial glaze."
        ),
        Product(
            id = 11,
            companyId = 3,
            companyName = "Cera",
            code = "CR-GAY-701",
            name = "Gayatri Bib Cock with Wall Flange",
            brand = "Cera",
            category = "CP",
            subcategory = "Tap",
            imageUri = "icon:tap",
            mrp = 1250.0,
            discountPercent = 28.0,
            gstPercent = 18.0,
            purchasePrice = 650.0,
            stockQuantity = 50,
            notes = "Quarter turn ceramic disc."
        ),
        Product(
            id = 12,
            companyId = 3,
            companyName = "Cera",
            code = "CR-SNK-402",
            name = "Handmade Satin Finish Kitchen Sink 24x18x9 Inch",
            brand = "Cera",
            category = "Kitchen",
            subcategory = "Kitchen Sink",
            imageUri = "icon:kitchen",
            mrp = 8900.0,
            discountPercent = 30.0,
            gstPercent = 18.0,
            purchasePrice = 4300.0,
            stockQuantity = 8,
            notes = "1.2mm thick SS304 with sound deadening pads."
        )
    )
}
