package proton.inject;

import static proton.inject.internal.util.Validator.checkNotNull;
import static proton.inject.internal.util.Validator.checkState;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import proton.inject.internal.binding.BindingBuilder;
import proton.inject.internal.binding.BindingBuilderImpl;
import proton.inject.internal.binding.Binding;
import proton.inject.internal.binding.Bindings;
import proton.inject.internal.provider.ApplicationProvider;
import proton.inject.internal.provider.ContextProvider;
import proton.inject.internal.provider.HandlerProvider;
import proton.inject.internal.provider.SystemServiceProvider;

public class DefaultModule implements Module {
	private Bindings mBindings;

	@Override
	public final synchronized void configure(Bindings bindings) {
		checkState(mBindings == null, "Re-entry is not allowed.");

		mBindings = checkNotNull(bindings, "bindings");
		try {
			configure();
		} finally {
			mBindings = null;
		}
	}

	protected void configure() {
		bind(Application.class).toProvider(ApplicationProvider.class).in(ApplicationScoped.class);
		bind(Context.class).toProvider(ContextProvider.class);
		bind(Handler.class).toProvider(HandlerProvider.class).in(ApplicationScoped.class);

		bind(ActivityManager.class).toProvider(new SystemServiceProvider<ActivityManager>(Context.ACTIVITY_SERVICE));
		bind(AlarmManager.class).toProvider(new SystemServiceProvider<AlarmManager>(Context.ALARM_SERVICE));
		bind(AudioManager.class).toProvider(new SystemServiceProvider<AudioManager>(Context.AUDIO_SERVICE));
		bind(ConnectivityManager.class).toProvider(
				new SystemServiceProvider<ConnectivityManager>(Context.CONNECTIVITY_SERVICE));
		bind(InputMethodManager.class).toProvider(
				new SystemServiceProvider<InputMethodManager>(Context.INPUT_METHOD_SERVICE));
		bind(KeyguardManager.class).toProvider(new SystemServiceProvider<KeyguardManager>(Context.KEYGUARD_SERVICE));

		bind(LocationManager.class).toProvider(new SystemServiceProvider<LocationManager>(Context.LOCATION_SERVICE));
		bind(NotificationManager.class).toProvider(
				new SystemServiceProvider<NotificationManager>(Context.NOTIFICATION_SERVICE));
		bind(PowerManager.class).toProvider(new SystemServiceProvider<PowerManager>(Context.POWER_SERVICE));
		bind(SensorManager.class).toProvider(new SystemServiceProvider<SensorManager>(Context.SENSOR_SERVICE));
		bind(TelephonyManager.class).toProvider(new SystemServiceProvider<TelephonyManager>(Context.TELEPHONY_SERVICE));
		bind(Vibrator.class).toProvider(new SystemServiceProvider<Vibrator>(Context.VIBRATOR_SERVICE));
		bind(WifiManager.class).toProvider(new SystemServiceProvider<WifiManager>(Context.WIFI_SERVICE));
		bind(WindowManager.class).toProvider(new SystemServiceProvider<WindowManager>(Context.WINDOW_SERVICE));
	}

	protected <T> BindingBuilder<T> bind(Class<T> clazz) {
		checkState(mBindings != null, "The Bindings can only be used inside configure()");
		Binding<T> binding = new Binding<T>(clazz);
		mBindings.add(binding);
		return new BindingBuilderImpl<T>(binding);
	}
}
