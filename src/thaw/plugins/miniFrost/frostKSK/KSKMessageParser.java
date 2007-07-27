package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.text.SimpleDateFormat;
import java.io.File;

import java.util.Vector;
import java.util.Iterator;
import java.util.List;

import frost.util.XMLTools;

import thaw.plugins.signatures.Identity;

import thaw.plugins.Hsqldb;
import thaw.core.Logger;

/**
 * Dirty parser reusing some Frost functions
 * (Note: dirty mainly because of the Frost format :p)
 */
public class KSKMessageParser {

	public KSKMessageParser() {

	}

	private String messageId;
	private String inReplyTo;
	private String from;
	private String subject;
	private String date;
	private String time;
	private String recipient;
	private String board;
	private String body;
	private String publicKey;
	private String signature;
	private String idLinePos;
	private String idLineLen;

	private Vector attachments;

	private Identity identity;

	private boolean alreadyInTheDb(Hsqldb db, String msgId) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKMessages "+
									 "WHERE msgId = ? LIMIT 1");
				st.setString(1, msgId);

				ResultSet res = st.executeQuery();

				return (res.next());
			}
		} catch(SQLException e) {
			Logger.error(this,
				     "Exception while checking if the message was already in the db: "+
				     e.toString());
			return false;
		}
	}



	public boolean insert(Hsqldb db,
			      int boardId, int rev,
			      String boardNameExpected) {
		if (boardNameExpected == null) {
			Logger.error(this, "Board name expected == null ?!");
			return false;
		}

		if (board == null
		    || !(boardNameExpected.toLowerCase().equals(board.toLowerCase())))
			return false;

		if (alreadyInTheDb(db, messageId)) {
			Logger.info(this, "We have already this id in the db ?!");
			return false;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.M.d HH:mm:ss");

		time = time.replaceAll("GMT", "").trim();
		date = date.trim();

		date += " "+time;

		java.util.Date dateUtil = dateFormat.parse(date, new java.text.ParsePosition(0));

		if (dateUtil == null) {
			if (date == null)
				date = "(null)";
			Logger.warning(this, "Unable to parse the date ?! : "+date);
		}

		java.sql.Timestamp dateSql = new java.sql.Timestamp(dateUtil.getTime());


		int replyToId = -1;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				/* we search the message to this one answer */
				if (inReplyTo != null) {
					String[] split = inReplyTo.split(",");
					inReplyTo = split[split.length-1];

					st = db.getConnection().prepareStatement("SELECT id FROM frostKSKMessages "+
										 "WHERE msgId = ? LIMIT 1");
					st.setString(1, inReplyTo);

					ResultSet res = st.executeQuery();

					if (res.next())
						replyToId = res.getInt("id");

				}

				/* we insert the message */

				st = db.getConnection().prepareStatement("INSERT INTO frostKSKMessages ("+
									 "subject, nick, sigId, content, "+
									 "date, msgId, inReplyTo, inReplyToId, "+
									 "rev, read, archived, boardId) VALUES ("+
									 "?, ?, ?, ?, "+
									 "?, ?, ?, ?, "+
									 "?, FALSE, FALSE, ?)");
				st.setString(1, subject);
				st.setString(2, from); /* nick */
				if (identity != null)
					st.setInt(3, identity.getId());
				else
					st.setNull(3, Types.INTEGER);
				st.setString(4, body); /* content */
				st.setTimestamp(5, dateSql);
				st.setString(6, messageId);

				if (replyToId >= 0)
					st.setInt(7, replyToId);
				else
					st.setNull(7, Types.INTEGER);

				if (inReplyTo != null)
					st.setString(8, inReplyTo);
				else
					st.setNull(8, Types.VARCHAR);

				st.setInt(9, rev);
				st.setInt(10, boardId);

				st.execute();


				/* we need the id of the message */

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKmessages "+
									 "WHERE msgId = ? LIMIT 1");
				st.setString(1, messageId);

				ResultSet set = st.executeQuery();

				set.next();

				int id = set.getInt("id");

				/* we insert the attachments */

				if (attachments != null) {
					for(Iterator it = attachments.iterator();
					    it.hasNext();) {
						KSKAttachment a = (KSKAttachment)it.next();
						a.insert(db, id);
					}
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't insert the message into the db because : "+e.toString());
			return false;
		}

		return true;
	}


	public final static char SIGNATURE_ELEMENTS_SEPARATOR = '|';

	private String getSignedContent() {
		final StringBuilder allContent = new StringBuilder();

		allContent.append(date).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(time).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(board).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(from).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(messageId).append(SIGNATURE_ELEMENTS_SEPARATOR);
		if( inReplyTo != null && inReplyTo.length() > 0 ) {
			allContent.append(inReplyTo).append(SIGNATURE_ELEMENTS_SEPARATOR);
		}
		if( recipient != null && recipient.length() > 0 ) {
			allContent.append(recipient).append(SIGNATURE_ELEMENTS_SEPARATOR);
		}
		allContent.append(idLinePos).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(idLineLen).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(subject).append(SIGNATURE_ELEMENTS_SEPARATOR);
		allContent.append(body).append(SIGNATURE_ELEMENTS_SEPARATOR);

		if (attachments != null) {
			for (Iterator it = attachments.iterator();
			     it.hasNext();) {
				KSKAttachment a = (KSKAttachment)it.next();
				allContent.append(a.getSignedStr());
			}
		}

		return allContent.toString();

	}


	public boolean checkSignature(Hsqldb db) {
		if (publicKey == null || signature == null) {
			from = from.replaceAll("@", "_");
			return true;
		}

		String[] split = from.split("@");
		if (split.length < 2 || "".equals(split[0].trim()))
			return false;

		String nick = split[0].trim();

		identity = Identity.getIdentity(db, nick, publicKey);

		return identity.check(getSignedContent(), signature);
	}


	protected boolean loadXMLElements(Element root) {
		messageId     = XMLTools.getChildElementsCDATAValue(root, "MessageId");
		inReplyTo     = XMLTools.getChildElementsCDATAValue(root, "InReplyTo");
		from          = XMLTools.getChildElementsCDATAValue(root, "From");
		subject       = XMLTools.getChildElementsCDATAValue(root, "Subject");
		date          = XMLTools.getChildElementsCDATAValue(root, "Date");
		time          = XMLTools.getChildElementsCDATAValue(root, "Time");
		recipient     = XMLTools.getChildElementsCDATAValue(root, "recipient");
		board         = XMLTools.getChildElementsCDATAValue(root, "Board");
		body          = XMLTools.getChildElementsCDATAValue(root, "Body");
		signature     = XMLTools.getChildElementsCDATAValue(root, "SignatureV2");
		publicKey     = XMLTools.getChildElementsCDATAValue(root, "pubKey");
		idLinePos     = XMLTools.getChildElementsTextValue(root, "IdLinePos");
		idLineLen     = XMLTools.getChildElementsTextValue(root, "IdLineLen");

		List l = XMLTools.getChildElementsByTagName(root, "AttachmentList");
		if (l.size() == 1) {
			attachments = new Vector();

			KSKAttachmentFactory factory = new KSKAttachmentFactory();

			Element attachmentsEl = (Element) l.get(0);
			Iterator i = XMLTools.getChildElementsByTagName(attachmentsEl,"Attachment").iterator();
			while (i.hasNext()){
				Element el = (Element)i.next();
				KSKAttachment attachment = factory.getAttachment(el);
				if (attachment != null)
					attachments.add(attachment);
			}
		}

		return true;
	}

	/**
	 * This function has been imported from FROST.
	 * Parses the XML file and passes the FrostMessage element to XMLize load method.
	 */
	public boolean loadFile(File file) {
		try {
			Document doc = null;
			try {
				doc = XMLTools.parseXmlFile(file, false);
			} catch(Exception ex) {  // xml format error
				Logger.warning(this, "Invalid Xml");
				return false;
			}

			if( doc == null ) {
				Logger.warning(this,
					       "Error: couldn't parse XML Document - " +
					       "File name: '" + file.getName() + "'");
				return false;
			}

			Element rootNode = doc.getDocumentElement();

			// load the message itself
			return loadXMLElements(rootNode);

		} catch(Exception e) {
			/* XMLTools throws runtime exception sometimes ... */
			Logger.warning(this, "Unable to parse XML message because : "+e.toString());
			e.printStackTrace();
			return false;
		}
	}



}
