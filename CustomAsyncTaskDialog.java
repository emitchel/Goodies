package sandhills.material.template.dealer.app.dialogs;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import sandhills.material.template.dealer.app.R;
import sandhills.material.template.dealer.app.cache.CacheBase;
import sandhills.material.template.dealer.app.classes.BundleConstants;
import sandhills.material.template.dealer.app.classes.Validation;
import sandhills.material.template.dealer.app.dialogs.DrillDownDialog.DrillDownPage;
import sandhills.material.template.dealer.app.enums.DataType;
import sandhills.material.template.dealer.app.helpers.JSONHelperWrapper;
import sandhills.material.template.dealer.app.utils.Utils;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnKeyListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * <p>
 * Tightly coupled with {@link JSONHelperWrapper}
 * <p>
 * Use this class if you want to have a simple popup display data given by a
 * data call
 * <p>
 * This should really be considered an abstract AsyncTask that handle popup
 * animations
 * 
 * @author elliot-mitchell
 * 
 * @param <T>
 */
public class CustomAsyncTaskDialog<T extends JSONHelperWrapper> extends
		DialogFragment {
	CustomDialogListener<T> mListener;
	GetData<T> oGetData;
	T mResultData;

	ListView mListView;
	Context mContext;
	String mTitle;
	RelativeLayout rlCancel;

	ProgressBar pb;

	public final static String TAG = "CustomAsyncTaskDialog";

	public interface CustomDialogListener<T> {
		/**
		 * Returning a listadapter populated with the helper class
		 * 
		 * @param result
		 * @return
		 */
		public ListAdapter createAdapter(T result);

		/**
		 * Callback for method to call when item selected
		 * 
		 * @param helper
		 * @param index
		 */
		public void onItemSelected(T helper, int index);

		/**
		 * Used to get an instance of the JSONHelperWrapper object
		 * 
		 * @return
		 */
		public T getJSONHelper();

		/**
		 * The network call to gather data.
		 * 
		 * @return
		 */
		public T request();
	}

	public static <T extends JSONHelperWrapper> CustomAsyncTaskDialog<T> newInstance(
			Context context, CustomDialogListener<T> listener, String sTitle) {
		return new CustomAsyncTaskDialog<T>(context, listener, sTitle);
	}

	public CustomAsyncTaskDialog(Context context,
			CustomDialogListener<T> listener, String sTitle) {

		this.mListener = listener;
		this.mTitle = sTitle;
		this.mContext = context;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private void cancelAsyncTask() {
		if (oGetData != null && oGetData.getStatus() != Status.FINISHED) {
			oGetData.cancel(true);
		}
	}

	@Override
	public void dismiss() {
		cancelAsyncTask();
		super.dismiss();
	}

	private View GatherViews(LayoutInflater inflater, ViewGroup container) {
		View v = inflater.inflate(R.layout.dialog_custom_async, container,
				false);
		rlCancel = (RelativeLayout) v.findViewById(R.id.cancel_dialog);
		pb = (ProgressBar) v.findViewById(R.id.pb);
		mListView = (ListView) v.findViewById(R.id.listView);
		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		outState.putString(BundleConstants.sTitle, this.mTitle);
		outState.putSerializable(BundleConstants.sT, this.mResultData);
		super.onSaveInstanceState(outState);
	}

	private void SetCurrentState(Bundle savedInstanceState, Bundle args) {
		if (savedInstanceState != null) {
			this.mTitle = savedInstanceState.getString(BundleConstants.sTitle);
			this.mResultData = (T) savedInstanceState
					.getSerializable(BundleConstants.sT);
		} else if (args != null) {
			// not currently set up to handle arguments

		}
	}

	private void SetUpObjects() {
		getDialog().setCancelable(true);

		getDialog().setTitle(mTitle);

	}

	private void SetCustomListeners() {

		rlCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

	}

	public void SetUpPage() {
		oGetData = new GetData<T>(pb);
		oGetData.execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = GatherViews(inflater, container);

		SetCurrentState(savedInstanceState, getArguments());

		SetUpObjects();

		SetCustomListeners();

		SetUpPage();

		return v;
	}

	
	
	private void populateResults(final T result) {
		if (result != null && result.bSuccess) {

			// Get Adapter from calling class
			ListAdapter adapter = mListener.createAdapter(result);

			if (adapter != null && adapter.getCount() > 0) {

				mListView.setAdapter(adapter);

				mListView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapter, View v,
							int position, long id) {
						dismiss();
						mListener.onItemSelected(result, position);
					}
				});

				// Fade in list view
				mListView.setVisibility(View.VISIBLE);
				mListView.setAlpha(0f);
				mListView.animate().alpha(1f)
						.setDuration(Utils.ALPHA_ANIMATION).setListener(null);

			} else {
				dismiss();
				throw new Error(
						"The adapter was not properly created for CustomAsyncTaskDialog");

			}
		} else {
			if(result!=null)
				result.getErrorAlertDialog(mContext).show();
			else {
				dismiss();
				throw new Error(
						"Houston we have a problem");
			}
		}

	}

	/**
	 * BUT WAIT, WHY USE TYPE 'Y' INSTEAD OF 'T' ?
	 * <p>
	 * That's because when we define the type 'T' in this class, we will
	 * inadvertently hide the global T, in which the listener only accepts the
	 * parent's 'T' type. If you still don't understand, change all the Y types
	 * in this class to T types and you'll see why
	 * 
	 * @author elliot-mitchell
	 * 
	 * @param <Y>
	 */
	private class GetData<Y extends JSONHelperWrapper> extends
			AsyncTask<String, Void, Y> {
		ProgressBar pb;
		CacheBase cache;
		String sData;
		DataType eType = DataType.NETWORK;

		public GetData(ProgressBar pb) {
			this.pb = pb;
		}

		@Override
		protected Y doInBackground(String... arg0) {
			try {
				// We need to check the cache for the corresponding
				// JSONHelperWrapper object first.
				Y obj = (Y) mListener.getJSONHelper();

				cache = obj.getCacheObject(mContext);
				sData = "";

				if (cache != null && cache.isStoredInCacheAndNotExpired()) {
					sData = cache.getFromCache();
				}

				if (Validation.isValidString(sData)) {
					obj.parseCacheStringData(sData);

					// we MUST do this otherwise the corresponding error
					// objects won't be set, and we can't handle errors
					// appropriately
					if (!obj.bSuccess) {
						// meaning the data was corrupt/couldn't be parsed
						obj = (Y) mListener.request();

					} else {;
						eType = DataType.CACHE;
					}

				} else {
					obj = (Y) mListener.request();

				}

				return obj;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Y result) {

			if (result == null) {
				//something went wrong
				Toast.makeText(mContext, mContext.getString(R.string.somethingwentwrong), Toast.LENGTH_LONG).show();
				dismiss();
			} else {
				
				if (cache != null && !cache.isStoredInCacheAndNotExpired())
					cache.commitToCache(result.originalDataString);

				mResultData = (T) result;
				populateResults((T) result);
			}
			
			if(Utils.testingWithTemplate(mContext))
				notifyDevOfRequest(eType,cache);
			
			//fade out progress bar			
			pb.animate().alpha(0f).setDuration(Utils.ALPHA_ANIMATION)
					.setListener(null);
		}

		@Override
		protected void onPreExecute() {
			pb.setVisibility(View.VISIBLE);
		}
	}

	private void notifyDevOfRequest( DataType eType, CacheBase cache) {
		
		switch(eType){
		case CACHE:
			Toast.makeText(mContext, "Pulled from cache - " + cache.getCacheAge() + " days old", Toast.LENGTH_LONG)
			.show();
			return;
		case NETWORK:
			Toast.makeText(mContext, "Network call", Toast.LENGTH_LONG).show();
			return;
		case DEFAULT:
			Toast.makeText(mContext, "Using default data", Toast.LENGTH_LONG)
			.show();
			return;
		}
		
	}
	

}
