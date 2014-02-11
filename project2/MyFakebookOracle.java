package project2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeSet;
import java.util.Vector;

public class MyFakebookOracle extends FakebookOracle {
	
	static String prefix = "yjtang.";
	
	// You must use the following variable as the JDBC connection
	Connection oracleConnection = null;
	
	// You must refer to the following variables for the corresponding tables in your database
	String cityTableName = null;
	String userTableName = null;
	String friendsTableName = null;
	String currentCityTableName = null;
	String hometownCityTableName = null;
	String programTableName = null;
	String educationTableName = null;
	String eventTableName = null;
	String participantTableName = null;
	String albumTableName = null;
	String photoTableName = null;
	String coverPhotoTableName = null;
	String tagTableName = null;
	
	
	// DO NOT modify this constructor
	public MyFakebookOracle(String u, Connection c) {
		super();
		String dataType = u;
		oracleConnection = c;
		// You will use the following tables in your Java code
		cityTableName = prefix+dataType+"_CITIES";
		userTableName = prefix+dataType+"_USERS";
		friendsTableName = prefix+dataType+"_FRIENDS";
		currentCityTableName = prefix+dataType+"_USER_CURRENT_CITY";
		hometownCityTableName = prefix+dataType+"_USER_HOMETOWN_CITY";
		programTableName = prefix+dataType+"_PROGRAMS";
		educationTableName = prefix+dataType+"_EDUCATION";
		eventTableName = prefix+dataType+"_USER_EVENTS";
		albumTableName = prefix+dataType+"_ALBUMS";
		photoTableName = prefix+dataType+"_PHOTOS";
		tagTableName = prefix+dataType+"_TAGS";
	}
	
	
	@Override
	// ***** Query 0 *****
	// This query is given to your for free;
	// You can use it as an example to help you write your own code
	//
	public void findMonthOfBirthInfo() throws SQLException{ 
		
		// Scrollable result set allows us to read forward (using next())
		// and also backward.  
		// This is needed here to support the user of isFirst() and isLast() methods,
		// but in many cases you will not need it.
		// To create a "normal" (unscrollable) statement, you would simply call
		// Statement stmt = oracleConnection.createStatement();
		//
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		
		// For each month, find the number of friends born that month
		// Sort them in descending order of count
		ResultSet rst = stmt.executeQuery("select count(*), month_of_birth from "+
				userTableName+
				" where month_of_birth is not null group by month_of_birth order by 1 desc");
		
		this.monthOfMostFriend = 0;
		this.monthOfLeastFriend = 0;
		this.totalFriendsWithMonthOfBirth = 0;
		
		// Get the month with most friends, and the month with least friends.
		// (Notice that this only considers months for which the number of friends is > 0)
		// Also, count how many total friends have listed month of birth (i.e., month_of_birth not null)
		//
		while(rst.next()) {
			int count = rst.getInt(1);
			int month = rst.getInt(2);
			if (rst.isFirst())
				this.monthOfMostFriend = month;
			if (rst.isLast())
				this.monthOfLeastFriend = month;
			this.totalFriendsWithMonthOfBirth += count;
		}
		
		// Get the names of friends born in the "most" month
		rst = stmt.executeQuery("select user_id, first_name, last_name from "+
				userTableName+" where month_of_birth="+this.monthOfMostFriend);
		while(rst.next()) {
			Long uid = rst.getLong(1);
			String firstName = rst.getString(2);
			String lastName = rst.getString(3);
			this.friendsInMonthOfMost.add(new UserInfo(uid, firstName, lastName));
		}
		
		// Get the names of friends born in the "least" month
		rst = stmt.executeQuery("select first_name, last_name, user_id from "+
				userTableName+" where month_of_birth="+this.monthOfLeastFriend);
		while(rst.next()){
			String firstName = rst.getString(1);
			String lastName = rst.getString(2);
			Long uid = rst.getLong(3);
			this.friendsInMonthOfLeast.add(new UserInfo(uid, firstName, lastName));
		}
		
		// Close statement and result set
		rst.close();
		stmt.close();
	}

	
	
	@Override
	// ***** Query 1 *****
	// Find information about friend names:
	// (1) The longest last name (if there is a tie, include all in result)
	// (2) The shortest last name (if there is a tie, include all in result)
	// (3) The most common last name, and the number of times it appears (if there is a tie, include all in result)
	//
	public void findNameInfo() throws SQLException { // Query1
		
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		
		ResultSet rst = stmt.executeQuery("select count(*), last_name from " +
				userTableName + " group by last_name order by 1 desc");
		
		this.mostCommonLastNamesCount = 0;
		
		while (rst.next()){
			int nameNum = rst.getInt(1);
			String lastName = rst.getString(2);
			
			//checks if the string is the longest
			if (!this.longestLastNames.isEmpty()){
				if (lastName.length() > this.longestLastNames.last().length()){
					this.longestLastNames.clear();
					this.longestLastNames.add(lastName);
				}
				else if (lastName.length() == this.longestLastNames.last().length()){
					this.longestLastNames.add(lastName);
				}
			}
			else{
				this.longestLastNames.add(lastName);
			}
			
			//checks if the string is the shortest
			if (!this.shortestLastNames.isEmpty()){
				if (lastName.length() < this.shortestLastNames.last().length()){
					this.shortestLastNames.clear();
					this.shortestLastNames.add(lastName);
				}
				else if (lastName.length() == this.shortestLastNames.last().length()){
					this.shortestLastNames.add(lastName);
				}
			}
			else{
				this.shortestLastNames.add(lastName);
			}
			
			if (nameNum > this.mostCommonLastNamesCount){
				this.mostCommonLastNames.clear();
				this.mostCommonLastNames.add(lastName);
				this.mostCommonLastNamesCount = nameNum;
			}
			else if (nameNum >= this.mostCommonLastNamesCount){
				this.mostCommonLastNames.add(lastName);
			}	
		}
		
		rst.close();
		stmt.close();
	}
	
	@Override
	// ***** Query 2 *****
	// Find the user(s) who have no friends in the network
	//
	// Be careful on this query!
	// Remember that if two users are friends, the friends table
	// only contains the pair of user ids once, subject to 
	// the constraint that user1_id < user2_id
	//
	public void lonelyFriends() throws SQLException {
		
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		
		ResultSet rst = stmt.executeQuery("select distinct user_id, first_name, last_name from " + userTableName + " where user_id NOT IN (select distinct user1_id from " + 
				friendsTableName + ") and user_id NOT IN (select distinct user2_id from " + friendsTableName + ")");
		
		this.countLonelyFriends = 0;
		
		while (rst.next()){
			Long userID = rst.getLong(1);
			String firstName = rst.getString(2);
			String lastName = rst.getString(3);
			this.lonelyFriends.add(new UserInfo(userID, firstName, lastName));
			this.countLonelyFriends += 1;
		}
		
		rst.close();
		stmt.close();
	}
	 

	@Override
	// ***** Query 3 *****
	// Find the users who still live in their hometowns
	// (I.e., current_city = hometown_city)
	//	
	public void liveAtHome() throws SQLException {
		
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		
		ResultSet rst = stmt.executeQuery("select distinct u.user_id, u.first_name, u.last_name from " + userTableName + " u, " + currentCityTableName + " c, " + 
				hometownCityTableName + " h where u.user_id = c.user_id and u.user_id = h.user_id and c.current_city_id = h.hometown_city_id");
		
		this.countLiveAtHome = 0;
		
		while (rst.next()){
			Long userID = rst.getLong(1);
			String firstName = rst.getString(2);
			String lastName = rst.getString(3);
			this.liveAtHome.add(new UserInfo(userID, firstName, lastName));
			this.countLiveAtHome += 1;
		}
		
		rst.close();
		stmt.close();
	}



	@Override
	// **** Query 4 ****
	// Find the top-n photos based on the number of tagged users
	// If there are ties, choose the photo with the smaller numeric PhotoID first
	// 
	public void findPhotosWithMostTags(int n) throws SQLException { 
		String photoId = "1234567";
		String albumId = "123456789";
		String albumName = "album1";
		String photoCaption = "caption1";
		String photoLink = "http://google.com";
		PhotoInfo p = new PhotoInfo(photoId, albumId, albumName, photoCaption, photoLink);
		TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
		tp.addTaggedUser(new UserInfo(12345L, "taggedUserFirstName1", "taggedUserLastName1"));
		tp.addTaggedUser(new UserInfo(12345L, "taggedUserFirstName2", "taggedUserLastName2"));
		this.photosWithMostTags.add(tp);
	}

	
	
	
	@Override
	// **** Query 5 ****
	// Find suggested "match pairs" of friends, using the following criteria:
	// (1) One of the friends is female, and the other is male
	// (2) Their age difference is within "yearDiff"
	// (3) They are not friends with one another
	// (4) They should be tagged together in at least one photo
	//
	// You should up to n "match pairs"
	// If there are more than n match pairs, you should break ties as follows:
	// (i) First choose the pairs with the largest number of shared photos
	// (ii) If there are still ties, choose the pair with the smaller user_id for the female
	// (iii) If there are still ties, choose the pair with the smaller user_id for the male
	//
	public void matchMaker(int n, int yearDiff) throws SQLException { 
		Long girlUserId = 123L;
		String girlFirstName = "girlFirstName";
		String girlLastName = "girlLastName";
		int girlYear = 1988;
		Long boyUserId = 456L;
		String boyFirstName = "boyFirstName";
		String boyLastName = "boyLastName";
		int boyYear = 1986;
		MatchPair mp = new MatchPair(girlUserId, girlFirstName, girlLastName, 
				girlYear, boyUserId, boyFirstName, boyLastName, boyYear);
		String sharedPhotoId = "12345678";
		String sharedPhotoAlbumId = "123456789";
		String sharedPhotoAlbumName = "albumName";
		String sharedPhotoCaption = "caption";
		String sharedPhotoLink = "link";
		mp.addSharedPhoto(new PhotoInfo(sharedPhotoId, sharedPhotoAlbumId, 
				sharedPhotoAlbumName, sharedPhotoCaption, sharedPhotoLink));
		this.bestMatches.add(mp);
	}

	
	
	// **** Query 6 ****
	// Suggest friends based on mutual friends
	// 
	// Find the top n pairs of users in the database who share the most
	// friends, but such that the two users are not friends themselves.
	//
	// Your output will consist of a set of pairs (user1_id, user2_id)
	// No pair should appear in the result twice; you should always order the pairs so that
	// user1_id < user2_id
	//
	// If there are ties, you should give priority to the pair with the smaller user1_id.
	// If there are still ties, give priority to the pair with the smaller user2_id.
	//
	@Override
	public void suggestFriendsByMutualFriends(int n) throws SQLException {
		Long user1_id = 123L;
		String user1FirstName = "Friend1FirstName";
		String user1LastName = "Friend1LastName";
		Long user2_id = 456L;
		String user2FirstName = "Friend2FirstName";
		String user2LastName = "Friend2LastName";
		FriendsPair p = new FriendsPair(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);

		p.addSharedFriend(567L, "sharedFriend1FirstName", "sharedFriend1LastName");
		p.addSharedFriend(678L, "sharedFriend2FirstName", "sharedFriend2LastName");
		p.addSharedFriend(789L, "sharedFriend3FirstName", "sharedFriend3LastName");
		this.suggestedFriendsPairs.add(p);
	}
	
	
	//@Override
	// ***** Query 7 *****
	// Given the ID of a user, find information about that
	// user's oldest friend and youngest friend
	// 
	// If two users have exactly the same age, meaning that they were born
	// on the same day, then assume that the one with the larger user_id is older
	//
	public void findAgeInfo(Long user_id) throws SQLException {
		
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		
		ResultSet rst = stmt.executeQuery("select u2.user_id, u2.first_name, u2.last_name, u2.year_of_birth, u2.month_of_birth, u2.day_of_birth from " + userTableName + " u1, " + userTableName + " u2, " +
				friendsTableName + " f where u1.user_id=" + user_id + " and ((u1.user_id = f.user1_id and u2.user_id = f.user2_id) or (u1.user_id = f.user2_id and u2.user_id = f.user1_id))");
		
		Long oldestAge = (long) 0;
		Long youngestAge = (long) 9999999;
		
		while (rst.next()){
			Long userID = rst.getLong(1);
			String firstName = rst.getString(2);
			String lastName = rst.getString(3);
			Long ageNum = 100*rst.getLong(4) + 100*rst.getLong(5) + rst.getLong(6);
			
			//handles if older
			if (ageNum > oldestAge ){
				this.oldestFriend = new UserInfo(userID, firstName, lastName);
				oldestAge = ageNum;
			}
			else if ((ageNum == oldestAge) && (userID > this.oldestFriend.userId)){
				this.oldestFriend = new UserInfo(userID, firstName, lastName);
			}
			
			//handles if younger
			if (ageNum < youngestAge){
				this.youngestFriend = new UserInfo(userID, firstName, lastName);
				youngestAge = ageNum;
			}
			else if ((ageNum == youngestAge) && (userID < this.youngestFriend.userId)){
				this.youngestFriend = new UserInfo(userID, firstName, lastName);
			}
		}
		
		//THE OUTPUT FOR THIS ONE IS CURRENTLY REVERSED BUT I HAVE NO IDEA WHY, THE LOGIC SEEMS CORRECT....
		
		rst.close();
		stmt.close();
	}
	
	
	@Override
	// ***** Query 8 *****
	// 
	// Find the name of the city with the most events, as well as the number of 
	// events in that city.  If there is a tie, return the names of all of the (tied) cities.
	//
	public void findEventCities() throws SQLException {
		this.eventCount = 12;
		this.popularCityNames.add("Ann Arbor");
		this.popularCityNames.add("Ypsilanti");
	}
	
	
	
	@Override
//	 ***** Query 9 *****
	//
	// Find pairs of potential siblings and print them out in the following format:
	//   # pairs of siblings
	//   sibling1 lastname(id) and sibling2 lastname(id)
	//   siblingA lastname(id) and siblingB lastname(id)  etc.
	//
	// A pair of users are potential siblings if they have the same last name and hometown, if they are friends, and
	// if they are less than 10 years apart in age.  Pairs of siblings are returned with the lower user_id user first
	// on the line.  They are ordered based on the first user_id and in the event of a tie, the second user_id.
	//  
	//
	public void findPotentialSiblings() throws SQLException {
		Long user1_id = 123L;
		String user1FirstName = "Friend1FirstName";
		String user1LastName = "Friend1LastName";
		Long user2_id = 456L;
		String user2FirstName = "Friend2FirstName";
		String user2LastName = "Friend2LastName";
		SiblingInfo s = new SiblingInfo(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);
		this.siblings.add(s);
	}
	
}
