package utils;

import controller.LibraryController;
import model.LibraryItem;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared GUI utilities used across all panels. */
public final class GuiUtils {

    private GuiUtils() {}

    // ── Catalogue table ───────────────────────────────────────────────────────

    /** Column names used by ViewPanel, AdminPanel, BorrowPanel, SearchSortPanel. */
    public static final String[] CATALOGUE_COLUMNS =
            {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};

    /** Converts one LibraryItem to an Object[] row matching CATALOGUE_COLUMNS order. */
    public static Object[] catalogueRow(LibraryItem item) {
        return new Object[]{
            item.getId(), item.getType(), item.getTitle(), item.getAuthor(),
            item.getYear(), item.getAvailableCopies(), item.getTotalCopies()
        };
    }

    /** Builds the full catalogue row list from the manager's inventory. */
    public static List<Object[]> buildCatalogueRows(LibraryController manager) {
        List<Object[]> rows = new ArrayList<>();
        for (LibraryItem item : manager.getInventory())
            rows.add(catalogueRow(item));
        return rows;
    }

    // ── Search filter ─────────────────────────────────────────────────────────

    /** Filters the table to show only items whose ID is in the matches list. Null clears the filter. */
    public static void applyItemFilter(TableRowSorter<gui.VirtualTableModel> sorter,
                                        List<LibraryItem> matches) {
        if (sorter == null) return;
        if (matches == null) { sorter.setRowFilter(null); return; }
        Set<String> ids = new HashSet<>();
        for (LibraryItem m : matches) ids.add(m.getId());
        sorter.setRowFilter(new RowFilter<gui.VirtualTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends gui.VirtualTableModel, ? extends Integer> entry) {
                return ids.contains(entry.getStringValue(0));
            }
        });
    }

    // ── Password confirmation ─────────────────────────────────────────────────

    /** Shows a password dialog and returns true only if the password is correct. */
    public static boolean confirmPassword(java.awt.Component parent,
                                          LibraryController manager,
                                          int userId, String promptText) {
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(parent, pf, promptText,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return false;
        boolean ok = manager.validatePassword(userId, new String(pf.getPassword()));
        if (!ok)
            JOptionPane.showMessageDialog(parent, "Incorrect password.",
                                          "Access Denied", JOptionPane.ERROR_MESSAGE);
        return ok;
    }
}
