package sandhills.material.template.dealer.app.helpers;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import sandhills.material.template.dealer.app.MainActivity;
import sandhills.material.template.dealer.app.R;
import sandhills.material.template.dealer.app.cache.CacheBase;
import sandhills.material.template.dealer.app.cache.CountryStateCache;
import sandhills.material.template.dealer.app.enums.Environment;
import sandhills.material.template.dealer.app.enums.ErrorAction;
import sandhills.material.template.dealer.app.enums.ErrorType;
import sandhills.material.template.dealer.app.network.JSONRequests;
import sandhills.material.template.dealer.app.utils.Utils;

public abstract class JSONHelperWrapper implements Serializable {
	private static final long serialVersionUID = 1L;

	public String sMessage;
	public String sDevMessage;
	public String sWebServiceMessage;
	public boolean bSuccess;
	public ErrorAction eErrorAction;

	public AlertListener mListener;

	public static final String MESSAGE = "sMessage";
	public static final String SUCCESS = "bSuccess";

	TextView tvDevErrorMessage;
	ImageButton ivErrorButton;
	TextView tvErrorMessage;
	TextView tvShowErrorDetails;

	boolean bExpanded = false;

	// Some APIs just don't return
	// these objects, the helpers
	// need a way of knowing
	boolean bSuccessObjectExisted = true;
	boolean bMessageObjectExisted = true;

	public boolean bSafeToDependOnSuccessProperty = true;

	float ROTATION_COUNT = 0;

	boolean bSetUpSuccessfully = false;

	public JSONHelperWrapper() {
	}

	/**
	 * 
	 * @param oJSON
	 * @throws JSONException
	 * 
	 *             <p>
	 *             This method will populate the Helper's main object
	 *             </p>
	 */
	public abstract void setUp(JSONObject oJSON) throws JSONException;

	/**
	 * 
	 * @param sJSONString
	 * @throws JSONException
	 * 
	 *             <p>
	 *             Some helpers extract the object differently
	 *             </p>
	 */
	public abstract JSONObject extractJSONObject(String sJSONString)
			throws JSONException;

	/**
	 * 
	 * @throws JSONException
	 * 
	 *             <p>
	 *             If the bSuccess object didn't exist, the helper must
	 *             determine if the data was really bad or not
	 *             </p>
	 * 
	 */
	public abstract void determineSuccessOnData(int nResponseCode)
			throws JSONException;

	/**
	 * <p>
	 * This method should return true if the helper class expected pre set data
	 * to exist prior to filling the corresponding objects
	 * </p>
	 * 
	 * @return bMissingRequiredDataInOrderToBuildObjects
	 */
	public abstract boolean bMissingRequiredDataInOrderToBuildObjects();

	/**
	 * <p>
	 * NOTE: If your helper class expects to make a POST request with it's
	 * related data, this method MUST be overwritten. Not making it abstract
	 * because not every helper needs JSON params
	 * </p>
	 * 
	 * @return
	 * @throws JSONException
	 */
	public JSONObject getJSONParams() throws JSONException {
		JSONObject jsonParams = null;
		return jsonParams;
	}

	/**
	 * This should be overwritten by subclass if it has a cache object
	 * associated w/ it <br>
	 * i.e. (a)Overrides public CountryStateCache getCacheObject(Context
	 * context){ return new CountryStateCache(context); }
	 * 
	 * @param context
	 * @return
	 */
	public <T extends CacheBase> T getCacheObject(Context context) {
		return null;
	}

	/**
	 * This is used for the error popup, you must pass a new instance to handle
	 * situations dynamically
	 * 
	 * @param oListener
	 */
	public void setAlertListener(AlertListener oListener) {
		this.mListener = oListener;
	}

	public interface AlertListener {
		public void onRetryAction();

		public void onCloseAction();
	}

	/**
	 * 
	 * @return bSafeToDependOnSuccessProperty
	 * 
	 *         <h1>The API generally sends back a bSuccess & sMessage BUT if
	 *         bSuccess isn't sent you shouldn't expect the data to be bad. This
	 *         property basically says, hey, check the data before running an
	 *         error handler, yo</h1>
	 */
	public boolean isItSafeToDependOnAPISuccessObject() {
		return this.bSafeToDependOnSuccessProperty;
	}

	// A loose API standard
	public void extractMessageAndSuccess(JSONObject json) throws JSONException {
		if (json.has(SUCCESS)) {
			this.bSuccess = json.getBoolean(SUCCESS);
		} else {
			this.bSuccess = this.bSuccessObjectExisted = false;
		}

		if (json.has(MESSAGE)) {

			this.sMessage = json.getString(MESSAGE);
		} else {
			this.sMessage = "";
			this.bMessageObjectExisted = false;
		}

		this.bSafeToDependOnSuccessProperty = this.bSuccessObjectExisted;

	}

	public void extractData(JSONObject json) throws JSONException {
		this.extractMessageAndSuccess(json);
		this.setUp(json);

		// At this point we can assume the
		// objects were built successfully
		// otherwise it would have caught an
		// exception by now
		this.bSetUpSuccessfully = true;
	}

	/**
	 * Just sets error messages and ErrorAction. You should overrride this
	 * method if you want to handle errors differently
	 * 
	 * @param oErrorHelper
	 * @param eErrorType
	 * @param sWebMethod
	 * @param e
	 * @param responseCode
	 * @param sFullRequestURL
	 * @param sJSONString
	 */
	public void handleErrorType(ErrorHelper oErrorHelper, ErrorType eErrorType,
			String sWebMethod, Exception e, int responseCode,
			String sFullRequestURL, String sJSONString) {

		// Need to set this if an exception called this method
		this.bSuccess = false;

		this.sWebServiceMessage = this.sMessage;

		this.sMessage = oErrorHelper.GetErrorMessage(eErrorType, sWebMethod, e,
				this.sWebServiceMessage, responseCode, sFullRequestURL,
				sJSONString);

		this.sDevMessage = oErrorHelper.GetDevFriendlyErrorMessage(eErrorType,
				sWebMethod, e, this.sWebServiceMessage, responseCode,
				sFullRequestURL, sJSONString);

		switch (eErrorType) {
		case LOGIC:
		case WS:
			this.eErrorAction = ErrorAction.OK;
			break;
		case HTTP:
		case JSON:
		case IO:
			this.eErrorAction = ErrorAction.RETRYCANCEL;
			break;
		}
	}

	/**
	 * Used for the error popup window.
	 */
	public OnClickListener expandArea = new OnClickListener() {

		@Override
		public void onClick(View v) {

			if (bExpanded) {
				// ivErrorButton.animate().rotation(ROTATION_COUNT);
				// ViewAnimationHelper.animateHeight(tvDevErrorMessage, 56, 0,
				// Utils.ALPHA_ANIMATION);
				ivErrorButton.setRotation(ROTATION_COUNT);
				tvDevErrorMessage.setVisibility(View.GONE);

			} else {
				// ivErrorButton.animate().rotation(ROTATION_COUNT);
				// ViewAnimationHelper.animateHeight(tvDevErrorMessage, 0, 56,
				// Utils.ALPHA_ANIMATION);
				ivErrorButton.setRotation(ROTATION_COUNT);
				tvDevErrorMessage.setVisibility(View.VISIBLE);
			}
			//
			ROTATION_COUNT += 180f;

			if (ROTATION_COUNT >= Float.MAX_VALUE)
				ROTATION_COUNT = 0f;

			bExpanded = !bExpanded;

		}
	};

	private AlertDialog.Builder getErrorAlertBuilder(
			AlertDialog.Builder ErrorAlert, Context c) {
		ScrollView ll = (ScrollView) ((Activity) c).getLayoutInflater()
				.inflate(R.layout.alertdialog_showdevmessage, null);

		tvErrorMessage = (TextView) ll.findViewById(R.id.errormessage);
		tvShowErrorDetails = (TextView) ll.findViewById(R.id.showdetails);
		ivErrorButton = (ImageButton) ll.findViewById(R.id.details_expand);
		tvDevErrorMessage = (TextView) ll.findViewById(R.id.deverrormessage);

		tvErrorMessage.setText(this.sMessage);
		tvDevErrorMessage.setText(this.sDevMessage);

		tvShowErrorDetails.setOnClickListener(expandArea);
		ivErrorButton.setOnClickListener(expandArea);

		ErrorAlert.setView(ll);

		return ErrorAlert;
	}

	/**
	 * <p>
	 * Use this method to get the appropriate error dialog associated with the
	 * object's current error.
	 * 
	 * <p>
	 * Set the AlertListener interface to handle the button actions
	 * 
	 * @param c
	 * @return
	 */
	public AlertDialog getErrorAlertDialog(Context c) {
		AlertDialog.Builder ErrorAlert = new AlertDialog.Builder(c,
				R.style.CustomErrorAlert);

		ErrorAlert.setTitle(c.getString(R.string.error));

		ErrorAlert = getErrorAlertBuilder(ErrorAlert, c);

		switch (this.eErrorAction) {
		case OK:
			ErrorAlert.setNegativeButton(c.getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							if (mListener != null)
								mListener.onCloseAction();

							dialog.dismiss();
						}
					});
			break;
		case RETRYCANCEL:
			ErrorAlert.setNegativeButton(c.getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mListener != null)
								mListener.onCloseAction();

							dialog.dismiss();
						}
					});
			ErrorAlert.setPositiveButton(c.getString(R.string.retry),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mListener != null)
								mListener.onRetryAction();

							dialog.dismiss();
						}
					});

			break;
		}

		return ErrorAlert.create();
	}

	/**
	 * The following methods can be used if you don't want the app to bomb if a
	 * given json object doesn't exist.
	 * 
	 */

	public int getInt(JSONObject json, String name) throws JSONException {

		if (json.has(name)) {
			return json.getInt(name);
		} else {
			return -1;
		}
	}

	public String getString(JSONObject json, String name) throws JSONException {
		if (json.has(name)) {
			return json.getString(name);
		} else {
			return "";
		}
	}

	public boolean getBoolean(JSONObject json, String name)
			throws JSONException {
		if (json.has(name)) {
			return json.getBoolean(name);
		} else {
			return false;
		}
	}

}
