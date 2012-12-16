/**
 *
 */
package fr.cedrik.inotes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author C&eacute;drik LIME
 */
class MeetingNoticeJSONConverter {
	protected static final Logger logger = LoggerFactory.getLogger(MeetingNoticeJSONConverter.class);

	public MeetingNoticeJSONConverter() {
	}

//	public Calendar convertJSON(InputStream input, Charset charset) throws IOException {
//		Map<String, JSONObject> map = new HashMap<String, JSONObject>();
//		try {
//			String responseBody = IOUtils.toString(input, charset);
//			logger.trace(responseBody);
//			// strip excess JavaScript
//			responseBody = responseBody.substring(responseBody.indexOf("DXX: ({") + "DXX: (".length(), responseBody.lastIndexOf(").item}"));
//			responseBody = RE.matcher(responseBody).replaceAll(">"); // responseBody.replace("\\>", ">");
//			JSONObject json = new JSONObject(responseBody);
//			responseBody = null;
//			// Convert JSON to Map
//			JSONArray jsonArray = json.getJSONArray("item");
//			for (int i = 0; i < jsonArray.length(); ++i) {
//				JSONObject obj = jsonArray.getJSONObject(i);
//				map.put(obj.getString("@name"), obj);
//			}
//		} catch (JSONException e) {
//			IOException ioe = new StreamCorruptedException();
//			ioe.initCause(e);
//			throw ioe;
//		}
//		Calendar ics = createCalendar(map);
//		return ics;
//	}
//	private static final Pattern RE = Pattern.compile("\\>", Pattern.LITERAL);
//
//	protected Calendar createCalendar(Map<String, JSONObject> json) {
//		Calendar calendar = new Calendar();
//		calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
//		calendar.getProperties().add(Version.VERSION_2_0);
//		calendar.getProperties().add(CalScale.GREGORIAN);
//		//TODO add events, etc..
//		TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
//		TimeZone timezone = registry.getTimeZone("Australia/Melbourne");
//		java.util.Calendar cal = java.util.Calendar.getInstance(timezone);
//		cal.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER);
//		cal.set(java.util.Calendar.DAY_OF_MONTH, 25);
//		DateTime dt = new DateTime(cal.getTime());
//		dt.setTimeZone(timezone);
//		VEvent christmas = new VEvent(dt, "Christmas Day");
//		// initialise as an all-day event..
//		christmas.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
//		calendar.getComponents().add(christmas);
//
//		calendar.getComponents().trimToSize();
//		try {
//			calendar.validate();
//		} catch (ValidationException e) {
//			throw new IllegalStateException(e);
//		}
//
//		if (logger.isTraceEnabled()) {
//			try {
//				CalendarOutputter outputter = new CalendarOutputter();
//				StringWriter out = new StringWriter(64);
//				outputter.output(calendar, out);
//				logger.trace(out.toString());
//			} catch (Exception e) {
//				logger.error("", e);
//			}
//		}
//
//		return calendar;
//	}
}
