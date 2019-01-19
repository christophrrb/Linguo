package linguo.example.com.linguo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by christophrrb on 1/9/19.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
	public final static String DATABASE_NAME = "linguo_database";
	public final static String TABLE_NAME = "user_info";
	public final static String ID = "id";
	public final static String NAME = "name";
	public final static String REASON_LEARNING = "reason_learning";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, 1);
		SQLiteDatabase db = this.getWritableDatabase();

	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME +
				" (id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name VARCHAR," +
				"reason_learning VARCHAR)");

	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(sqLiteDatabase);
	}
}
