package proton.inject.widget;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

import proton.inject.Proton;

/**
 * @author keishin.yokomaku
 */
public class ProtonArrayAdapter<T> extends ArrayAdapter<T> {
    public ProtonArrayAdapter(Context context, int resource) {
        super(context, resource);
        Proton.getInjector(context).inject(this);
    }

    public ProtonArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        Proton.getInjector(context).inject(this);
    }

    public ProtonArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, objects);
        Proton.getInjector(context).inject(this);
    }

    public ProtonArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, objects);
        Proton.getInjector(context).inject(this);
    }

    public ProtonArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
        Proton.getInjector(context).inject(this);
    }

    public ProtonArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        Proton.getInjector(context).inject(this);
    }
}
