import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service managing inventory operations for Kingpin MNL.
 * Enforces the invariant that products are always maintained in sorted order by product code.
 */
public class InventoryService {
    private final String inventoryFilePath;
    private final List<Product> products;

    public InventoryService(String inventoryFilePath) {
        this.inventoryFilePath = inventoryFilePath;
        this.products = new ArrayList<>();
        loadAndSortInventory();
    }

    public void loadAndSortInventory() {
        this.products.clear();
        List<Product> loaded = FileStorage.loadInventory(inventoryFilePath);
        this.products.addAll(loaded);

        // Sort by product code using Insertion Sort
        SortAlgorithms.insertionSortProductsByCode(this.products);
    }

    public void saveInventory() {
        FileStorage.saveInventory(inventoryFilePath, this.products);
    }

    public Product findByCode(String code) {
        int index = SearchAlgorithms.binarySearchProductByCode(products, code);
        if (index >= 0) {
            return products.get(index);
        }
        return null;
    }

    public List<Product> searchByName(String query) {
        return SearchAlgorithms.linearSearchProductsByName(products, query);
    }

    public boolean addProduct(Product product) {
        if (product == null || product.getCode().isEmpty()) {
            return false;
        }
        if (findByCode(product.getCode()) != null) {
            return false;
        }

        SortAlgorithms.insertInSortedOrder(products, product);
        saveInventory();
        return true;
    }

    public boolean recordStockReceived(String code, int quantity) {
        Product p = findByCode(code);
        if (p == null || quantity <= 0) {
            return false;
        }
        p.addStock(quantity);
        saveInventory();
        return true;
    }

    public boolean deductStock(String code, int quantity) {
        Product p = findByCode(code);
        if (p == null || p.getStock() < quantity) {
            return false;
        }
        p.deductStock(quantity);
        saveInventory();
        return true;
    }

    public boolean restoreStock(String code, int quantity) {
        Product p = findByCode(code);
        if (p == null) {
            return false;
        }
        p.addStock(quantity);
        saveInventory();
        return true;
    }

    public boolean updateProduct(String code, String newName, String newCategory, double newPrice, int newThreshold) {
        Product p = findByCode(code);
        if (p == null) {
            return false;
        }
        p.setName(newName);
        p.setCategory(newCategory);
        p.setPrice(newPrice);
        p.setLowStockThreshold(newThreshold);
        saveInventory();
        return true;
    }

    public boolean removeProduct(String code) {
        int index = SearchAlgorithms.binarySearchProductByCode(products, code);
        if (index >= 0) {
            products.remove(index);
            saveInventory();
            return true;
        }
        return false;
    }

    public List<Product> getLowStockProducts() {
        List<Product> lowStock = new ArrayList<>();
        for (Product p : products) {
            if (p.isLowStock()) {
                lowStock.add(p);
            }
        }
        return lowStock;
    }

    public List<Product> getAllProducts() {
        return Collections.unmodifiableList(products);
    }
}
