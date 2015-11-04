package sandhills.material.template.dealer.app.dialogs;

import sandhills.material.template.dealer.app.R;
import sandhills.material.template.dealer.app.classes.BundleConstants;
import sandhills.material.template.dealer.app.dialogs.DrillDownDialog.DrillDownPage;
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
		public ListAdapter createAdapter(T result);

		public void onItemSelected(T helper, int index);

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
				Toast.makeText(mContext, "No items were provided for list",
						Toast.LENGTH_LONG).show();
			}
		} else {
			result.getErrorAlertDialog(mContext).show();
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

		public GetData(ProgressBar pb) {
			this.pb = pb;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Y doInBackground(String... arg0) {
			try {
				return (Y) mListener.request();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Y result) {
			mResultData = (T) result;
			populateResults((T) result);
			pb.animate().alpha(0f).setDuration(Utils.ALPHA_ANIMATION)
					.setListener(null);
		}

		@Override
		protected void onPreExecute() {
			pb.setVisibility(View.VISIBLE);
		}
	}

}
