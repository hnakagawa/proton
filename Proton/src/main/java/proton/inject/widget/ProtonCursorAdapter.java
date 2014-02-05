package proton.inject.widget;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;

import proton.inject.Proton;

/**
 * @author keishin.yokomaku
 */
public abstract class ProtonCursorAdapter extends CursorAdapter {
    public static final String TAG = ProtonCursorAdapter.class.getSimpleName();

    public ProtonCursorAdapter(Context context, Cursor c) {
        super(context, c);
        Proton.getInjector(context).inject(this);
    }

    public ProtonCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        Proton.getInjector(context).inject(this);
    }

    public ProtonCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        Proton.getInjector(context).inject(this);
    }
}
