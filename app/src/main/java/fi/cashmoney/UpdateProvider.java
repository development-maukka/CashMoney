package fi.cashmoney;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.List;

/** Read-only temporary URI grants for the app-owned update APK. */
public final class UpdateProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File resolve(Uri uri) throws FileNotFoundException {
        List<String> parts = uri.getPathSegments();
        if (parts.size() != 2 || !(parts.get(0).equals("updates") && parts.get(1).equals("cashmoney-update.apk"))) throw new FileNotFoundException();
        File root = getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);if(root==null)throw new FileNotFoundException();
        File file = new File(root, parts.get(1));
        try {
            if (!file.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()) || !file.isFile()) throw new FileNotFoundException();
        } catch (IOException e) { throw new FileNotFoundException(); }
        return file;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        try {
            File file = resolve(uri);
            String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) values[i] = OpenableColumns.DISPLAY_NAME.equals(columns[i]) ? file.getName() : OpenableColumns.SIZE.equals(columns[i]) ? file.length() : null;
            MatrixCursor cursor = new MatrixCursor(columns, 1); cursor.addRow(values); return cursor;
        } catch (FileNotFoundException e) { return null; }
    }
    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
}
