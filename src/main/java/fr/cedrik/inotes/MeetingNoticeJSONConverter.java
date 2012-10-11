/**
 *
 */
package fr.cedrik.inotes;

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

/**
 * @author C&eacute;drik LIME
 */
class MeetingNoticeJSONConverter {
	protected static final Logger logger = LoggerFactory.getLogger(MeetingNoticeJSONConverter.class);

	public MeetingNoticeJSONConverter() {
	}

	public Calendar convertJSON(InputStream input, Charset charset) throws IOException {
		Map<String, JSONObject> map = new HashMap<String, JSONObject>();
		try {
			String responseBody = IOUtils.toString(input, charset);
			logger.trace(responseBody);
			// strip excess JavaScript
			responseBody = responseBody.substring(responseBody.indexOf("DXX: ({") + "DXX: (".length(), responseBody.lastIndexOf(").item}"));
			responseBody = RE.matcher(responseBody).replaceAll(">"); // responseBody.replace("\\>", ">");
			JSONObject json = new JSONObject(responseBody);
			responseBody = null;
			// Convert JSON to Map
			JSONArray jsonArray = json.getJSONArray("item");
			for (int i = 0; i < jsonArray.length(); ++i) {
				JSONObject obj = jsonArray.getJSONObject(i);
				map.put(obj.getString("@name"), obj);
			}
		} catch (JSONException e) {
			IOException ioe = new StreamCorruptedException();
			ioe.initCause(e);
			throw ioe;
		}
		Calendar ics = createCalendar(map);
		return ics;
	}
	private static final Pattern RE = Pattern.compile("\\>", Pattern.LITERAL);

	protected Calendar createCalendar(Map<String, JSONObject> json) {
		Calendar calendar = new Calendar();
		calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);
		//TODO add events, etc..
		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone timezone = registry.getTimeZone("Australia/Melbourne");
		java.util.Calendar cal = java.util.Calendar.getInstance(timezone);
		cal.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER);
		cal.set(java.util.Calendar.DAY_OF_MONTH, 25);
		DateTime dt = new DateTime(cal.getTime());
		dt.setTimeZone(timezone);
		VEvent christmas = new VEvent(dt, "Christmas Day");
		// initialise as an all-day event..
		christmas.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
		calendar.getComponents().add(christmas);

		try {
			calendar.validate();
		} catch (ValidationException e) {
			throw new IllegalStateException(e);
		}

		if (logger.isTraceEnabled()) {
			try {
				CalendarOutputter outputter = new CalendarOutputter();
				StringWriter out = new StringWriter(64);
				outputter.output(calendar, out);
				logger.trace(out.toString());
			} catch (Exception e) {
				logger.error("", e);
			}
		}

		return calendar;
	}
}
