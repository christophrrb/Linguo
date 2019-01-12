package linguo.example.com.linguo;

import android.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

public class ChatActivity extends SingleFragmentActivity {
	@Override
	protected Fragment createFragment() {
		return ChatFragment.newInstance();
	}
}
