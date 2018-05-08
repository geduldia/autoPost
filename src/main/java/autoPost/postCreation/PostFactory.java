package autoPost.postCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import autoPost.entities.Post;
import autoPost.entities.PostGroup;

public class PostFactory {
	//a file with regexes of all accepted date-types and their date-formats
		private File dateFormatsFile = new File("src/main/resources/dateTimeFormats/dateTimeFormats.txt");
		// the current year is needed to calculate the next possible tweet-date.
		private int currentYear;
		// regexes for the accepted date and time formats
		private List<String> dateTimeRegexes = new ArrayList<String>();
		// regexes for the accepted date formats
		private List<String> dateRegexes = new ArrayList<String>();
		// accepted date and time formats
		private List<String> dateFormats = new ArrayList<String>();
		// a formatter to normalize the different input formats
		private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		/**
		 * sets the current year and reads the accepted formats for date-inputs from dateTimeFormats.txt
		 */
		public PostFactory(String dateFormatsPath) {
			currentYear = LocalDateTime.now().getYear();
			this.dateFormatsFile = new File(dateFormatsPath);
			readDateFormatsFromFile();
		}

		/**
		 * read date-formats from file
		 */
		private void readDateFormatsFromFile() {
			try {
				BufferedReader in = new BufferedReader(new FileReader(dateFormatsFile));
				String line = in.readLine();
				while(line!= null){
					String[] split = line.split("\t");
					if(line.startsWith("DateTime:")){
						addDateTimeForm(split[1], split[2]);
					}
					if(line.startsWith("Date:")){
						addDateForm(split[1], split[2]);
					}
					line = in.readLine();
				}
				in.close();
			} catch (IOException e) {
				System.out.println("couldnt read dateFormats from File");
				e.printStackTrace();
			}
		}

		/**
		 *
		 * adds a new dateTime format
		 *
		 * @param regex
		 *            - dateTime-regex
		 * @param format
		 *            - dateTime-format
		 */
		private void addDateTimeForm(String regex, String format) {
			dateTimeRegexes.add(regex);
			dateFormats.add(format);
		}

		/**
		 * adds a new date format
		 *
		 * @param regex
		 *            - date-regex
		 * @param format
		 *            - date-format
		 */
		private void addDateForm(String regex, String format) {
			dateRegexes.add(regex);
			dateFormats.add(format);
		}

		/**
		 * creates a TweetGroup-object from a tsv-file by building a tweet for each
		 * row, which has the following format: [date] tab [time(optional)]
		 * tab [tweet-content] tab [imageUrl (optional)] tab [latitude (optional)]
		 * tab [longitude (optional)]
		 *
		 * @param tsvFile
		 *            the input file
		 * @param title
		 *            - a title for the created tweetGroup
		 * @param description
		 *            - a description for the created tweetGroup
		 * @param delay
		 *            - the number of years between the written date in the file and
		 *            the calculated tweet-date
		 * @return a new tweetGroup with a tweet for each row in the file
		 */
		public PostGroup getPostsFromTSVFile(File tsvFile, String title, String description, int delay, String encoding) throws MalformedTSVFileException {
			PostGroup group = new PostGroup(title, description);
			try {
				System.out.println(tsvFile.getAbsolutePath());
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tsvFile), encoding));
				String line = in.readLine();
				String content;
				String date;
				String time;
				int delayInSeconds = -1;
				LocalDateTime lastLDT = null;
				boolean useDelay = false;
				LocalDateTime ldt;
				Post post;
				int row = 1;
				while (line != null) {
					if(line.equals("")){
						line = in.readLine();
						row++;
						continue;
					}
					if(line.toLowerCase().startsWith("date")||line.toLowerCase().startsWith("datum")){
						line = in.readLine();
						row++;
						continue;
					}
					String[] split = line.split("\t");
					// get tweet-date
					date = split[0].trim();
					String origDate = date;
					if(date.toLowerCase().contains("delay")){
						useDelay = true;
					}
					else if(date.length() <= 7){		
						//add missing day of month
						date = date.concat("-01");
					}
					time = split[1].trim();
					if(!useDelay){
						if (time.equals("")) {
							ldt = parseDateString(date);
							if(ldt == null){
								throw new MalformedTSVFileException(row, 1, date, "malformed date: "+origDate+"  (row: "+row+" column: 1)");
							}
						} else {
							ldt = parseDateString(date + " " + time);
							if(ldt == null){
								throw new MalformedTSVFileException(row, 1, date + " " + time, "malformed date or time: "+date + " " + time+"  (row: "+row+" column: 1-2)");
							}
						}
					}
					else{
					
							try{
								delayInSeconds = Integer.parseInt(time);
							}
							catch(NumberFormatException e){
								if(delayInSeconds == -1){
									throw new MalformedTSVFileException(row, 2, time, "malformed/missing delay: "+time+"  (row "+ row+ "column: 2)");
								}
							}
						
						
						ldt = lastLDT.plusSeconds(delayInSeconds);
					}

					// get tweet-image
					String imageUrl = null;
					if (split.length > 3) {
						imageUrl = split[3];
						if(imageUrl.length() > 0){
							try {
								Resource image = new UrlResource(imageUrl);
							} catch (Exception e) {
								throw new MalformedTSVFileException(row, 4, imageUrl, "invalid image-Url: "+imageUrl+" (row: "+row+" column: 4)");
							}
						}
					}
					
					// get tweet-content
					content = split[2];
					//escape java
					content = StringEscapeUtils.unescapeJava(content);

					// add delay
					ldt = ldt.plusYears(delay);


					if (delay == 0) {
						while (ldt.isBefore(LocalDateTime.now())) {
							ldt = ldt.plusYears(1);
						}
					}
					// normalize date to the format yyyy-MM-dd HH:mm
					String formattedDate = ldt.format(formatter);
					// set default time to 12:00
					boolean midnight = false;
					if (time.contains(" 00:00")) {
						midnight = true;
					}
					if (!midnight) {
						formattedDate = formattedDate.replace(" 00:00", " 12:00");
					}
					if (ldt.isAfter(LocalDateTime.now())) {
						post = new ImportedPost(formattedDate, content, imageUrl, group);
						((ImportedPost) post).setTrimmed(content.length() > 140);
						group.addPost(post);
					}
					line = in.readLine();
					row++;
					lastLDT = ldt;
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
	
			return group;
		}
		
		/**
		 * trims a sentence to a tweet-lenth of max. 140 characters and adds the
		 * given url to the tweets content
		 *
		 * @param toTrim
		 * @param url
		 * @return a valid tweet content
		 */
		public String trimToTweet(String toTrim, String url, String imageUrl) {
			int maxLength = 140;
			if(url!= null){
				maxLength = 116;
			}
			if (toTrim.length() > maxLength) {
				
				toTrim = toTrim.substring(0, maxLength);
				toTrim = toTrim.substring(0, toTrim.lastIndexOf(" "));
			}
			if (url != null) {
				toTrim = toTrim.concat(" " + url);
			}
			return toTrim;
		}
		
		public String trimToTweet(String toTrim, String imageUrl){
			String urlRegex = "(http(s)?:\\/\\/(.*))(\\s)?" ;
			Pattern pattern = Pattern.compile(urlRegex);
			Matcher matcher = pattern.matcher(toTrim);
			String url = null;
			if(matcher.find()){
				url = matcher.group(1);
				toTrim = toTrim.replace(url, "");
			}	
			return trimToTweet(toTrim, url, imageUrl);
		}

		/**
		 * extract date-strings from a TimeML annotated sentence. Extracts only dates
		 * with at least a year and month.
		 *
		 * @param sentence
		 * @return a list of date-expressions
		 */
		private List<String> extractDates(String sentence) {
			List<String> dates = new ArrayList<String>();
			String dateRegex = "type=\"DATE\" value=\"([0-9|XXXX]{4}-[0-9]{2}(-[0-9]{2})?)\">";
			String timeRegex = "type=\"TIME\" value=\"(([0-9]{4}|XXXX)-[0-9]{2}(-[0-9]{2})?)(( [A-Z]{2,4})|(T[0-9]{2}:[0-9]{2}(:[0-9]{2})?))\">";
			Pattern pattern = Pattern.compile(dateRegex);
			Matcher matcher = pattern.matcher(sentence);
			Pattern pattern2 = Pattern.compile(timeRegex);
			Matcher matcher2 = pattern2.matcher(sentence);
			String date = null;
			String time = null;
			while (matcher.find()) {
				date = matcher.group(1);
				// select only date with at least a month
				if (date.contains("-")) {
					dates.add(date);
				}
				while (matcher2.find()) {
					time = matcher2.group(1) + matcher2.group(6).replace("T", " ");
					dates.add(time);
				}
			}
			return dates;
		}

		/**
		 * calculates the next possible tweet-date (= next anniversary in the
		 * future) from the given date-expression (e.g. 1955-08-01  returns
		 * 2017-08-01 )
		 *
		 * @param pastDate past date
		 * @return next anniversary in the format YYYY-MM-dd HH:mm
		 */
		private String getTweetDate(String pastDate) {
			// set default time to 12:00
			boolean midnight = false;
			if (pastDate.contains(" 00:00")) {
				midnight = true;
			}
			// handle dates with unspecified year
			if (pastDate.startsWith("XXXX")) {
				pastDate = pastDate.replace("XXXX", currentYear + "");
			}
			// set default day in month to 01
			if (pastDate.length() == 7) {
				pastDate = pastDate.concat("-01");
			}
			LocalDateTime ldtOriginal = parseDateString(pastDate);
			if (ldtOriginal == null)
				return null;
			// find next anniverary in the future
			LocalDateTime ldt = LocalDateTime.of(currentYear, ldtOriginal.getMonth(), ldtOriginal.getDayOfMonth(),
					ldtOriginal.getHour(), ldtOriginal.getMinute());
			LocalDateTime today = LocalDateTime.now();
			if (ldt.isBefore(today)) {
				ldt = ldt.plusYears(1);
			}
			// normalize date to the format YYYY-MM-dd HH:mm
			String formattedDate = ldt.format(formatter);
			if (!midnight) {
				formattedDate = formattedDate.replace(" 00:00", " 12:00");
			}
			return formattedDate;
		}

		/**
		 * parses a date-string and returns a LocalDateTime-object of the format
		 * YYYY-MM-dd HH:mm
		 *
		 * @param date
		 * @return a LocalDateTime-object of the given date-string or null if the
		 *         string does not satisfy one of the accepted date-formats
		 */
		private LocalDateTime parseDateString(String date) {
			LocalDateTime ldt;
			LocalDate ld;
			Pattern pattern;
			Matcher matcher;
			for (int i = 0; i < dateTimeRegexes.size(); i++) {
				String regex = dateTimeRegexes.get(i);
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(date);
				if (matcher.find()) {
					DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(i));
					ldt = LocalDateTime.parse(date, dtFormatter);
					return ldt;
				}
			}
			int dateTimes = dateTimeRegexes.size();
			for (int j = 0; j < dateRegexes.size(); j++) {
				String regex = dateRegexes.get(j);
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(date);
				if (matcher.find()) {
					DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(j + dateTimes));
					ld = LocalDate.parse(date, dtFormatter);
					ldt = LocalDateTime.of(ld, LocalTime.of(12, 0));
					return ldt;
				}
			}
			return null;
		}
}
