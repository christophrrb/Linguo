package linguo.example.com.linguo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by christophrrb on 12/23/18.
 */

public abstract class SingleFragmentActivity extends AppCompatActivity {
	protected abstract Fragment createFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fragment);

		android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.chat_fragment);

		if (fragment == null) {
			fragment = createFragment();

			fm.beginTransaction()
					.add(R.id.chat_fragment, fragment)
					.commit();
		}
	}
}
