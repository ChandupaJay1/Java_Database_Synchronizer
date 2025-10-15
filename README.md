# 🧠 Database Synchronization System for Java & MySQL

Developed by **Chandupa Jayalath – NerdTech**

![Java](https://img.shields.io/badge/Java-11%2B-blue?style=for-the-badge&logo=java)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-orange?style=for-the-badge&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

A powerful and practical synchronization engine for local & cloud-based MySQL databases — built with precision by Chandupa Jayalath.

![Screenshot of a model.](https://myoctocat.com/assets/images/base-octocat.svg)

---

## 🚀 Overview

This project provides a **bidirectional synchronization system** between a **Local** and an **Online MySQL Database**, designed for the DR_Fashions desktop application. It ensures that both databases remain consistent, allowing the application to function seamlessly even when offline. The system automatically handles connection fallbacks and efficiently manages data integrity between environments.

## ✨ Features

- ✅ **Automatic Connection Fallback**: Seamlessly switches from the **Online → Local** database when an internet connection is unavailable, ensuring uninterrupted application use.
- 🔁 **Flexible Sync Directions**: Supports both **One-way** (master-slave) and **Two-way** (master-master) synchronization models.
- 🔒 **Foreign Key Safe**: Processes tables in a predefined dependency order to prevent foreign key constraint violations during synchronization.
- ⚡ **High-Performance**: Utilizes **batch data transfers** and `INSERT ... ON DUPLICATE KEY UPDATE` statements for rapid and efficient data synchronization.
- 💬 **Real-time Status Updates**: Provides live progress feedback through a flexible callback system, which can be hooked into a UI or logged to the console.
- 🧩 **Modular Architecture**: Built with a clean, modular, and reusable Java architecture, making it easy to integrate and maintain.

## 🧠 Architecture

The synchronization logic is primarily handled by two main classes, each with a distinct responsibility:

| Class                | Purpose                                                                |
| -------------------- | ---------------------------------------------------------------------- |
| `DatabaseConnection` | Handles database connections, one-way sync operations, and status reporting. |
| `DatabaseSync`       | Manages the complete two-way synchronization logic between Local ↔ Online databases. |

## 🔄 How It Works

### 1. Automatic Connection Fallback

The system prioritizes the online database. If the connection fails, it automatically switches to the local database to keep the application fully operational. A status message is logged to indicate the change:

### 2. Synchronization Logic

#### 🟢 One-Way Sync (`DatabaseConnection`)
This process is used for pushing data in a single direction (e.g., Local → Online).
1.  Temporarily disables foreign key checks on the target database to prevent dependency errors.
2.  Reads all data from the source database table.
3.  Processes records in batches (e.g., 100 records per batch) for optimal performance.
4.  Uses an `INSERT ... ON DUPLICATE KEY UPDATE` pattern to efficiently insert new records and update existing ones.
    ```sql
    INSERT INTO target_table (id, col1, col2) VALUES (?, ?, ?)
    ON DUPLICATE KEY UPDATE col1 = VALUES(col1), col2 = VALUES(col2);
    ```
5.  Re-enables foreign key checks after the operation is complete.

#### 🔵 Two-Way Sync (`DatabaseSync`)
This is the core process that ensures both databases are perfectly mirrored.
1.  **Local → Online Sync**: The system first performs a one-way sync from the local database to the online database. This pushes all local changes (created/updated while offline) to the cloud.
2.  **Online → Local Sync**: Next, it performs a one-way sync in the reverse direction (Online → Local). This pulls any changes made on other systems to the local machine.
3.  **Record-Level Comparison**: During each sync direction, records are compared by their primary key.
    - If a record exists in the source but not the target, it is **inserted**.
    - If a record exists in both, it is **updated** in the target.
    - Duplicate key errors are gracefully ignored.
4.  At the end of the process, both databases are identical.

### 3. Table Dependency Order

To maintain data integrity, tables are synchronized in an order that respects foreign key relationships. Parent tables are always processed before their dependent child tables.

**Example Order:**
`role` → `type` → `user` → `employee` → `stock` → `accesories` → `attendence` → `monthly_payment` → `per_day_salary`

## 🛠️ Installation

1.  Clone the repository:
    ```bash
    git clone <repository-url>
    ```
2.  Add the source files from `/src/NerdTech/DR_Fashion/DatabaseConnection/` to your Java project.
3.  Ensure you have the MySQL Connector/J driver in your project's classpath.
4.  Configure your local and online database credentials within the `DatabaseConnection` class.

## 💡 Usage Example

Here is a basic example of how to initiate a full two-way synchronization with console feedback.

```java
import NerdTech.DR_Fashion.DatabaseConnection.DatabaseSync;

public class SyncManager {

    public static void main(String[] args) {
        // Define a callback to print status messages to the console
        DatabaseSync.StatusCallback statusCallback = message -> System.out.println(message);

        try {
            // Instantiate the sync manager with the callback
            DatabaseSync synchronizer = new DatabaseSync(statusCallback);

            // Start the full two-way synchronization process
            System.out.println("🚀 Starting full two-way database synchronization...");
            synchronizer.syncAll();
            System.out.println("✅ Synchronization complete!");

        } catch (Exception e) {
            System.err.println("❌ An error occurred during synchronization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 🧩 Core Components API

**1.DatabaseConnection.java:**

| Method                | Description                                                                |
| -------------------- | ---------------------------------------------------------------------- |
| `getLocalConnection()` | Establishes and returns a connection to the local MySQL database.|
| `getOnlineConnection()`       | Establishes and returns a connection to the online (Cloud) MySQL database. |
| `getConnection()` | Attempts to connect to the online DB first, then falls back to the local DB.|
| `syncLocalToOnline()`       | Performs a one-way synchronization of all tables from Local → Online. |
| `syncOnlineToLocal()` | Performs a one-way synchronization of all tables from Online → Local.|
| `setStatusCallback()`       | Sets a callback function to display live sync progress in the UI or console. |

**2.DatabaseConnection.java:**

| Method                | Description                                                                |
| -------------------- | ---------------------------------------------------------------------- |
| `syncAll()	` | Executes the full two-way synchronization process for all defined tables.|
| `syncTable()`       | Synchronizes a single table in both directions (Local → Online, then Online → Local). |
| `syncDirection()` | Handles the core logic for a one-way sync, including record comparison and updates.|
| `insertRecord()`       | Inserts a new record into the target database when it's missing. |
| `updateRecord()` | Updates an existing record in the target database when a mismatch is found.|

----

## 🧭 Future Enhancements

  1.`Conflict Resolution:` Implement timestamp-based (last_updated) conflict resolution to handle cases where the same record is modified in both databases.
  
  2.`Transactional Sync:` Wrap synchronization blocks in database transactions for atomic operations, ensuring that a sync process either completes fully or not at all.
  
  3.`Detailed Logging:` Introduce a comprehensive logging system for easier debugging and error reporting.
  
  4.`Incremental Sync:` Optimize the system to only sync records that have changed since the last synchronization, reducing data transfer.
  
  5.`UI Integration:` Develop a dedicated UI component to display real-time sync progress and allow manual sync triggers.

----

## 📂 Repository Structure
```
/src/NerdTech/DR_Fashion/DatabaseConnection/
├── DatabaseConnection.java   # Handles one-way sync + DB connections
├── DatabaseSync.java         # Manages full two-way synchronization
└── README.md                 # Project documentation

```

 ## 🧑‍💻 Developed By

   **Chandupa Jayalath**

      🎓 Software Engineering Undergraduate – Java Institute for Advanced Technology
      
      🏢 NerdTech | DR_Fashions Project
      
      📧 chandupajayalath20@gmail.com
      
## 📜 License

    This project is open-source and available under the MIT License. Feel free to use, modify, and enhance it with proper attribution.
  
   ### **Instructions to Create the File:**

    1.  **Copy** all the text inside the code block above.
    2.  Open a simple text editor (like Notepad, VS Code, Sublime Text, or TextEdit).
    3.  **Paste** the copied text into the editor.
    4.  Click **File -> Save As...**
    5.  For the "File name", type exactly: `README.md`
    6.  For the "Save as type" or "Format", select **All Files**. This is important to prevent it from saving as `README.md.txt`.
    7.  **Save** the file in your project's main directory.
