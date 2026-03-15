package controller;

/** Observer — implement in any panel that needs to refresh after state mutations. */
public interface LibraryChangeListener {
    void onLibraryDataChanged();
}
