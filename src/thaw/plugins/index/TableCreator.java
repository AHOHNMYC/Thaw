package thaw.plugins.index;

import java.sql.*;

import thaw.core.*;
import thaw.plugins.Hsqldb;

/**
 * Create all the tables used to save the indexes.
 * <br/>
 * "Comprenne qui pourra" :P
 *
 * <pre>
 * indexCategories (name, positionInTree)
 *  |-- indexCategories (name, positionInTree)
 *  | |-- [...]
 *  |
 *  |-- indexes (name, publicKey, [privateKey], positionInTree)
 *    |-- fileCategories (name, [positionInTree])
 *    | |-- fileCategories (name, [positionInTree])
 *    | | |-- [...]
 *    | |
 *    | |-- links (indexName, indexPublicKey)
 *    | |-- files (publicKey, mime, size)
 *    |   |-- metadatas (name, value)
 *    |
 *    |-- links (indexName, indexPublicKey)
 *    |-- files (publicKey, mime, size)
 *      |-- metadatas (name, value)
 * </pre>
 *
 * positionInTree == position in its JTree branch.
 */
public class TableCreator {

	public TableCreator() {

	}

	/**
	 * Can be safely called, even if the tables already exist.
	 */
	public static void createTables(Hsqldb db) {
		sendQuery(db, 
			  "CREATE CACHED TABLE indexCategories ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "positionInTree INTEGER NOT NULL,"
			  + "parent INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (parent) REFERENCES indexCategories (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE indexes ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "originalName VARCHAR(255) NOT NULL,"
			  + "displayName VARCHAR(255) NULL,"
			  + "publicKey VARCHAR(255) NOT NULL,"
			  + "privateKey VARCHAR(255) NULL,"
			  + "positionInTree INTEGER NOT NULL,"
			  + "parent INTEGER NULL,"			  
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (parent) REFERENCES indexCategories (id))");
		
		sendQuery(db,
			  "CREATE CACHED TABLE fileCategories ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "positionInTree INTEGER NULL," /* can be null if the user doesn't own the index */
			  + "catParent INTEGER NULL,"
			  + "indexParent INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (catParent) REFERENCES fileCategories (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE files ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "publicKey VARCHAR(350)," // key ~= 100 + filename == 255 max
			  + "mime VARCHAR(50),"
			  + "size BIGINT,"
			  + "catParent INTEGER NULL,"
			  + "indexParent INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (catParent) REFERENCES fileCategories (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE links ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "indexName VARCHAR(255),"
			  + "publicKey VARCHAR(350)," // key ~= 100 + filename == 255 max
			  + "catParent INTEGER NULL,"
			  + "indexParent INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (catParent) REFERENCES fileCategories (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE metadataNames ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "PRIMARY KEY (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE metadatas ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "nameId INTEGER NOT NULL,"
			  + "value VARCHAR(255) NOT NULL,"
			  + "fileId INTEGER NOT NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (fileId) REFERENCES files (id),"
			  + "FOREIGN KEY (nameId) REFERENCES metadataNames (id))");

	}

	public static void dropTables(Hsqldb db) {
		sendQuery(db, "DROP TABLE metadatas");
		sendQuery(db, "DROP TABLE metadataNames");

		sendQuery(db, "DROP TABLE files");
		sendQuery(db, "DROP TABLE links");
		
		sendQuery(db, "DROP TABLE fileCategories");
		sendQuery(db, "DROP TABLE indexes");
		sendQuery(db, "DROP TABLE indexCategories");				
	}


	/**
	 * Returns no error / Throws no exception.
	 */
	protected static void sendQuery(Hsqldb db, String query) {
		try {
			db.executeQuery(query);
		} catch(SQLException e) {
			Logger.notice(new TableCreator(), "While (re)creating sql tables: "+e.toString());
		}
	}
}