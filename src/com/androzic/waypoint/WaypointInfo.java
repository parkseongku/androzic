/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2012  Andrey Novikov <http://andreynovikov.info/>
 *
 * This file is part of Androzic application.
 *
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Androzic.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic.waypoint;

import java.io.File;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androzic.Androzic;
import com.androzic.R;
import com.androzic.data.Waypoint;
import com.androzic.util.Geo;
import com.androzic.util.StringFormatter;

public class WaypointInfo extends Activity implements OnClickListener
{
	private Waypoint waypoint;
	int index;
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.act_waypoint_info);

        index = getIntent().getExtras().getInt("INDEX");
        double lat = getIntent().getExtras().getDouble("lat");
        double lon = getIntent().getExtras().getDouble("lon");
        
		Androzic application = (Androzic) getApplication();
		waypoint = application.getWaypoint(index);
		
		setTitle(waypoint.name);
		if (waypoint.drawImage)
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
			Bitmap b = BitmapFactory.decodeFile(application.iconPath + File.separator + waypoint.image, options);
			if (b != null)
			{
				b.setDensity(Bitmap.DENSITY_NONE);
				setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(getResources(), b));
			}
			else
			{
				setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_map);
			}
		}
		else
		{
			setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_map);
		}

		WebView description = (WebView) findViewById(R.id.description);
		
		if ("".equals(waypoint.description))
		{
			description.setVisibility(View.GONE);
		}
		else
		{
			String descriptionHtml;
			try
			{
				TypedValue tv = new TypedValue();
				Theme theme = getTheme();
				Resources resources = getResources();
				theme.resolveAttribute(android.R.attr.textColorSecondary, tv, true);
				int secondaryColor = resources.getColor(tv.resourceId);
				String css = String.format("<style type=\"text/css\">html,body{margin:0;background:transparent} *{color:#%06X}</style>\n", (secondaryColor & 0x00FFFFFF));
				descriptionHtml = css + waypoint.description;
				description.setWebViewClient(new WebViewClient()
				{
				    @Override
				    public void onPageFinished(WebView view, String url)
				    {
				    	view.setBackgroundColor(Color.TRANSPARENT);
				        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				        	view.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
				    }
				});
				description.setBackgroundColor(Color.TRANSPARENT);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	description.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
			}
			catch (Resources.NotFoundException e)
			{
				description.setBackgroundColor(Color.LTGRAY);
				descriptionHtml = waypoint.description;
			}
			
			WebSettings settings = description.getSettings();
			settings.setDefaultTextEncodingName("utf-8");
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
			{
				String base64 = Base64.encodeToString(descriptionHtml.getBytes(), Base64.DEFAULT);
				description.loadData(base64, "text/html; charset=utf-8", "base64");
			}
			else
			{
				description.loadData("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + descriptionHtml, "text/html; chartset=UTF-8", null);
			}
		}

		String coords = StringFormatter.coordinates(application.coordinateFormat, " ", waypoint.latitude, waypoint.longitude);
		((TextView) findViewById(R.id.coordinates)).setText(coords);
		
		if (waypoint.altitude != Integer.MIN_VALUE)
		{
			String altitude = String.format(Locale.getDefault(), "%d %s", waypoint.altitude, getResources().getStringArray(R.array.distance_abbrs_short)[2]);
			((TextView) findViewById(R.id.altitude)).setText(altitude);
		}
		
		double dist = Geo.distance(lat, lon, waypoint.latitude, waypoint.longitude);
		double bearing = Geo.bearing(lat, lon, waypoint.latitude, waypoint.longitude);
		bearing = application.fixDeclination(bearing);
		String distance = StringFormatter.distanceH(dist)+" "+StringFormatter.bearingH(bearing);
		((TextView) findViewById(R.id.distance)).setText(distance);

		if (waypoint.date != null)
			((TextView) findViewById(R.id.date)).setText(DateFormat.getDateFormat(this).format(waypoint.date)+" "+DateFormat.getTimeFormat(this).format(waypoint.date));
		else
			((TextView) findViewById(R.id.date)).setVisibility(View.GONE);
			
	    ((ImageButton) findViewById(R.id.navigate_button)).setOnClickListener(this);
	    ((ImageButton) findViewById(R.id.edit_button)).setOnClickListener(this);
	    ((ImageButton) findViewById(R.id.share_button)).setOnClickListener(this);
	    ((ImageButton) findViewById(R.id.remove_button)).setOnClickListener(this);
    }

	@Override
    public void onClick(View v)
    {
		setResult(Activity.RESULT_OK, new Intent().putExtra("index", index).putExtra("action", v.getId()));
   		finish();
    }

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		waypoint = null;
	}

}