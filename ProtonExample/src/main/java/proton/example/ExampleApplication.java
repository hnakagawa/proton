package proton.example;

import android.app.Application;

import proton.inject.DefaultModule;
import proton.inject.Proton;

public class ExampleApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Proton.initialize(this, new DefaultModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(Greeting.class).to(GreetingImpl.class);
			}
		});
	}
}
