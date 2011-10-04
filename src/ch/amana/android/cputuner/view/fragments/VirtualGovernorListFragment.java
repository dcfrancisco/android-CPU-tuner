package ch.amana.android.cputuner.view.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import ch.amana.android.cputuner.R;
import ch.amana.android.cputuner.helper.GeneralMenuHelper;
import ch.amana.android.cputuner.helper.Logger;
import ch.amana.android.cputuner.helper.SettingsStorage;
import ch.amana.android.cputuner.hw.PowerProfiles;
import ch.amana.android.cputuner.model.ModelAccess;
import ch.amana.android.cputuner.provider.db.DB;
import ch.amana.android.cputuner.provider.db.DB.VirtualGovernor;
import ch.amana.android.cputuner.view.activity.HelpActivity;

public class VirtualGovernorListFragment extends ListFragment {

	private Cursor displayCursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		displayCursor = getActivity().managedQuery(DB.VirtualGovernor.CONTENT_URI, DB.VirtualGovernor.PROJECTION_DEFAULT, null, null, DB.VirtualGovernor.SORTORDER_DEFAULT);

	}

	@Override
	public void onResume() {
		super.onResume();


		SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.virtual_governor_item, displayCursor,
				new String[] { DB.VirtualGovernor.NAME_VIRTUAL_GOVERNOR_NAME, DB.VirtualGovernor.NAME_REAL_GOVERNOR,
						DB.VirtualGovernor.NAME_GOVERNOR_THRESHOLD_DOWN, DB.VirtualGovernor.NAME_GOVERNOR_THRESHOLD_UP },
				new int[] { R.id.tvVirtualGovernor, R.id.tvGorvernor, R.id.tvThresholdDown, R.id.tvThresholdUp });

		adapter.setViewBinder(new ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == VirtualGovernor.INDEX_VIRTUAL_GOVERNOR_NAME) {
					long virtGovId = PowerProfiles.getInstance().getCurrentProfile().getVirtualGovernor();
					int color = Color.LTGRAY;
					if (virtGovId == cursor.getLong(DB.INDEX_ID)) {
						color = getResources().getColor(R.color.cputuner_green);
					}

					if (!SettingsStorage.getInstance().isUseVirtualGovernors()) {
						color = Color.DKGRAY;
					}
					((TextView) view).setTextColor(color);
				} else if (columnIndex == VirtualGovernor.INDEX_GOVERNOR_THRESHOLD_UP) {
					if (cursor.getInt(columnIndex) < 1) {
						((TextView) view).setText("");
						((View) view.getParent()).findViewById(R.id.labelThresholdUp).setVisibility(View.INVISIBLE);
						return true;
					}
					((View) view.getParent()).findViewById(R.id.labelThresholdUp).setVisibility(View.VISIBLE);
				}else if (columnIndex == VirtualGovernor.INDEX_GOVERNOR_THRESHOLD_DOWN) {
					if (cursor.getInt(columnIndex) < 1) {
						((TextView) view).setText("");
						((View) view.getParent()).findViewById(R.id.labelThresholdDown).setVisibility(View.INVISIBLE);
						return true;
					}
					((View) view.getParent()).findViewById(R.id.labelThresholdDown).setVisibility(View.VISIBLE);
				}
				return false;
			}
		});
		
		setListAdapter(adapter);
		getListView().setOnCreateContextMenuListener(this);
		getListView().setEnabled(SettingsStorage.getInstance().isUseVirtualGovernors());
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Uri uri = ContentUris.withAppendedId(DB.VirtualGovernor.CONTENT_URI, id);

		startActivity(new Intent(Intent.ACTION_EDIT, uri));

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.list_option, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (handleCommonMenu(item)) {
			return true;
		}
		if (GeneralMenuHelper.onOptionsItemSelected(getActivity(), item, HelpActivity.PAGE_VIRTUAL_GOVERNOR)) {
			return true;
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.db_list_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);

		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Logger.e("bad menuInfo", e);
			return false;
		}

		final Uri uri = ContentUris.withAppendedId(DB.VirtualGovernor.CONTENT_URI, info.id);
		switch (item.getItemId()) {
		case R.id.menuItemDelete:
			deleteVirtualGovernor(uri);
			return true;

		case R.id.menuItemEdit:
			startActivity(new Intent(Intent.ACTION_EDIT, uri));
			return true;
		}

		if (handleCommonMenu(item)) {
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private boolean handleCommonMenu(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuItemInsert:
			startActivity(new Intent(Intent.ACTION_INSERT, DB.VirtualGovernor.CONTENT_URI));
			return true;
		}
		return false;
	}

	private void deleteVirtualGovernor(final Uri uri) {
		final Activity act = getActivity();
		Builder alertBuilder = new AlertDialog.Builder(act);
		if (ModelAccess.getInstace(act).isVirtualGovernorUsed(ContentUris.parseId(uri))) {
			// no not delete
			alertBuilder.setTitle(R.string.menuItemDelete);
			alertBuilder.setMessage(R.string.msgDeleteVirtGovNotPossible);
			alertBuilder.setNegativeButton(android.R.string.ok, null);
		} else {
			alertBuilder.setTitle(R.string.menuItemDelete);
			alertBuilder.setMessage(R.string.msg_delete_selected_item);
			alertBuilder.setNegativeButton(android.R.string.no, null);
			alertBuilder.setPositiveButton(android.R.string.yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					act.getContentResolver().delete(uri, null, null);
				}
			});
		}
		AlertDialog alert = alertBuilder.create();
		alert.show();
	}
}