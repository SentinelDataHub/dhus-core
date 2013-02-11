--   Data Hub Service (DHuS) - For Space data distribution.
--   Copyright (C) 2014-2020 GAEL Systems

--   This file is part of DHuS software sources.

--   This program is free software: you can redistribute it and/or modify
--   it under the terms of the GNU Affero General Public License as
--   published by the Free Software Foundation, either version 3 of the
--   License, or (at your option) any later version.

--   This program is distributed in the hope that it will be useful,
--   but WITHOUT ANY WARRANTY; without even the implied warranty of
--   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
--   GNU Affero General Public License for more details.

--   You should have received a copy of the GNU Affero General Public License
--   along with this program. If not, see <http://www.gnu.org/licenses/>.

/* KEYSTORES *************************************************************************/
INSERT INTO PUBLIC.KEYSTOREENTRIES(KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES ('store1','key1','unaltered','value1')
INSERT INTO PUBLIC.KEYSTOREENTRIES(KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES ('store1','key2','unaltered','value2')
INSERT INTO PUBLIC.KEYSTOREENTRIES(KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES ('store1','key3','unaltered','value3')
INSERT INTO PUBLIC.KEYSTOREENTRIES(KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES ('store2','key1','unaltered','value1')
INSERT INTO PUBLIC.KEYSTOREENTRIES(KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES ('store3','key1','unaltered','value1')
INSERT INTO PUBLIC.KEYSTOREENTRIES(KEYSTORE, ENTRYKEY, TAG, VALUE) VALUES ('store3','key2','unaltered','value3')
/* Add users *************************************************************************/
INSERT INTO PUBLIC.USERS(UUID, LOGIN, PASSWORD, PASSWORD_ENCRYPTION, CREATED, UPDATED) VALUES('00000000000000000000000000000000', 'root', 'password', 'NONE', NOW(), NOW());
INSERT INTO PUBLIC.USERS(UUID, LOGIN, PASSWORD, PASSWORD_ENCRYPTION, CREATED, UPDATED, EMAIL) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'koko', 'koko', 'NONE', '2014-06-03 15:35:05.037000', '2014-06-03 15:35:05.037000', 'koko@kokoFactories.fr');
INSERT INTO PUBLIC.USERS(UUID, LOGIN, PASSWORD, PASSWORD_ENCRYPTION, CREATED, UPDATED) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'riko', 'koko', 'NONE', '2014-06-03 15:35:05.037000', '2014-06-03 15:35:05.037000');
INSERT INTO PUBLIC.USERS(UUID, LOGIN, PASSWORD, PASSWORD_ENCRYPTION, CREATED, UPDATED) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'toto', 'koko', 'NONE', '2014-06-03 15:35:05.037000', '2014-06-03 15:35:05.037000');
INSERT INTO PUBLIC.USERS(UUID, LOGIN, PASSWORD, PASSWORD_ENCRYPTION, CREATED, UPDATED) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 'babar', 'koko', 'NONE', '2014-06-03 15:35:05.037000', '2014-06-03 15:35:05.037000');
-- add roles AUTHED, SEARCH, DOWNLOAD, UPLOAD, DATA_MANAGER, USER_MANAGER, SYSTEM_MANAGER, ARCHIVE_MANAGER, STATISTICS
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'AUTHED')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'SEARCH')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'DOWNLOAD')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'AUTHED')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'AUTHED')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 'AUTHED')
-- root user
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'AUTHED')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'SEARCH')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'DOWNLOAD')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'UPLOAD')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'DATA_MANAGER')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'USER_MANAGER')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'SYSTEM_MANAGER')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'ARCHIVE_MANAGER')
INSERT INTO PUBLIC.USER_ROLES(USER_UUID, ROLES) VALUES('00000000000000000000000000000000', 'STATISTICS')

/* Preferences ***********************************************************************************/
INSERT INTO PREFERENCES VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PREFERENCES VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1')
INSERT INTO PREFERENCES VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2')
INSERT INTO PREFERENCES VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3')
-- set users preferences 1 3 2 0
UPDATE PUBLIC.USERS AS u SET u.PREFERENCES_UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0' WHERE u.UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0'
UPDATE PUBLIC.USERS AS u SET u.PREFERENCES_UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1' WHERE u.UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1'
UPDATE PUBLIC.USERS AS u SET u.PREFERENCES_UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2' WHERE u.UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2'
UPDATE PUBLIC.USERS AS u SET u.PREFERENCES_UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3' WHERE u.UUID = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3'

/* Add Products **********************************************************************************/
INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ONLINE) VALUES(0, '2014-06-05 15:35:05.037000', '2014-06-05 21:35:05.037000', FALSE , 'prod0', 128, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ONLINE) VALUES(1, '2014-06-05 15:35:05.037000', '2014-06-05 20:35:05.037000', TRUE, 'prod1', 256, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ONLINE) VALUES(2, '2014-06-02 15:35:05.037000', '2014-06-05 19:35:05.037000', FALSE, 'prod2', 512,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ONLINE) VALUES(3, '2014-06-03 15:35:05.037000', '2014-06-05 18:35:05.037000', FALSE, 'prod3', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, ORIGIN, SIZE, UUID, ONLINE) VALUES(4, '2014-06-06 15:35:05.037000', '2014-06-12 17:35:05.037000', TRUE,'prod4', 'space', 1042,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ONLINE) VALUES(5, '2014-06-07 15:35:05.037000', '2014-06-16 16:35:05.037000', FALSE, 'prod5', 2048, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ORIGIN, ONLINE) VALUES(6, '2014-06-07 15:35:05.037000', '2014-06-16 15:35:05.037000', FALSE, 'prod6', 4096, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa6', 'space', TRUE)

INSERT INTO PUBLIC.PRODUCTS(ID, CREATED, UPDATED, LOCKED, IDENTIFIER, SIZE, UUID, ORIGIN, ONLINE) VALUES(7, '2014-06-07 15:35:05.037000', '2014-06-16 15:35:05.037000', FALSE, 'prod7', 4096, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa7', 'space/invaders', TRUE)

/* CHECKSUM **************************************************************************************/
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (6, 'MD5','abc')
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (6, 'SHA-1', 'acb')
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (6, 'SHA-256', 'bac')
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (2, 'MD5', 'MON MD5 PRODUCT 2')
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (7, 'MD5','abc')
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (7, 'SHA-1', 'acb')
INSERT INTO PUBLIC.CHECKSUMS(PRODUCT_ID, DOWNLOAD_CHECKSUM_ALGORITHM, DOWNLOAD_CHECKSUM_VALUE) VALUES (7, 'SHA-256', 'bac')

/* Add MetadataDefinition */
INSERT INTO PUBLIC.METADATA_DEFINITION(ID, NAME, TYPE, CATEGORY, QUERYABLE) VALUES(0, 'Size ', 'text/plain', '', '')
INSERT INTO PUBLIC.METADATA_DEFINITION(ID, NAME, TYPE, CATEGORY, QUERYABLE) VALUES(1, 'Name ', 'text/plain', '', '')
INSERT INTO PUBLIC.METADATA_DEFINITION(ID, NAME, TYPE, CATEGORY, QUERYABLE) VALUES(2, 'deletable ', 'text/plain', '', '')
INSERT INTO PUBLIC.METADATA_DEFINITION(ID, NAME, TYPE, CATEGORY, QUERYABLE) VALUES(3, 'updatable', 'text/plain', '', '')

/* Add MetadataIndex *****************************************************************************/
INSERT INTO PUBLIC.METADATA_INDEXES(VALUE, PRODUCT_ID, METADATA_DEFINITION_ID) VALUES('1GB', 2, 0)
INSERT INTO PUBLIC.METADATA_INDEXES(VALUE, PRODUCT_ID, METADATA_DEFINITION_ID) VALUES('metadata', 2, 1)
INSERT INTO PUBLIC.METADATA_INDEXES(VALUE, PRODUCT_ID, METADATA_DEFINITION_ID) VALUES('2GB', 1, 0)
INSERT INTO PUBLIC.METADATA_INDEXES(VALUE, PRODUCT_ID, METADATA_DEFINITION_ID) VALUES('meta', 1, 1)
INSERT INTO PUBLIC.METADATA_INDEXES(VALUE, PRODUCT_ID, METADATA_DEFINITION_ID) VALUES('test', 6, 2)
INSERT INTO PUBLIC.METADATA_INDEXES(VALUE, PRODUCT_ID, METADATA_DEFINITION_ID) VALUES('test', 7, 3)

/* Add Collections + root collection *************************************************************/
INSERT INTO PUBLIC.COLLECTIONS(UUID, NAME, DESCRIPTION) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', '#.root', 'Root of all the collections')
INSERT INTO PUBLIC.COLLECTIONS(UUID, NAME) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'Asia')
INSERT INTO PUBLIC.COLLECTIONS(UUID, NAME) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'Africa')
INSERT INTO PUBLIC.COLLECTIONS(UUID, NAME) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 'Japan')
INSERT INTO PUBLIC.COLLECTIONS(UUID, NAME) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', 'China')
INSERT INTO PUBLIC.COLLECTIONS(UUID, NAME) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa5', 'SouthAfrica')
-- insert products in collection
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 0)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 1)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 2)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 3)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 5)

INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 4)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 5)

INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 0)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 1)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', 2)

INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', 1)
INSERT INTO PUBLIC.COLLECTION_PRODUCT(COLLECTIONS_UUID, PRODUCTS_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', 3)
/* Add Searches **********************************************************************************/
INSERT INTO PUBLIC.SEARCHES(UUID, COMPLETE, FOOTPRINT, NOTIFY, VALUE) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', NULL, NULL, FALSE, 'value0')
INSERT INTO PUBLIC.SEARCHES(UUID, COMPLETE, FOOTPRINT, NOTIFY, VALUE) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', NULL, NULL, TRUE, 'value1')
INSERT INTO PUBLIC.SEARCHES(UUID, COMPLETE, FOOTPRINT, NOTIFY, VALUE) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', NULL, NULL, TRUE, 'value2')
INSERT INTO PUBLIC.SEARCHES(UUID, COMPLETE, FOOTPRINT, NOTIFY, VALUE) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', NULL, NULL, FALSE, 'value3')
-- add advances
INSERT INTO PUBLIC.SEARCH_ADVANCED(SEARCH_UUID, ADVANCED, ADVANCED_KEY) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'advanceValue', 'advanceKey')
-- add search preferences
INSERT INTO PUBLIC.SEARCH_PREFERENCES(PREFERENCE_UUID, SEARCHES_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PUBLIC.SEARCH_PREFERENCES(PREFERENCE_UUID, SEARCHES_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2')
INSERT INTO PUBLIC.SEARCH_PREFERENCES(PREFERENCE_UUID, SEARCHES_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1')

/* Add AccessRestriction *************************************************************************/
INSERT INTO PUBLIC.ACCESS_RESTRICTION(ACCESS_RESTRICTION, UUID, EXPIRED, BLOCKING_REASON) VALUES('locked', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', TRUE, 'punition1')
INSERT INTO PUBLIC.ACCESS_RESTRICTION(ACCESS_RESTRICTION, UUID, EXPIRED, BLOCKING_REASON) VALUES('expired', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', FALSE, 'too late')
INSERT INTO PUBLIC.ACCESS_RESTRICTION(ACCESS_RESTRICTION, UUID, EXPIRED, BLOCKING_REASON) VALUES('locked', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', FALSE, 'punition2')
INSERT INTO PUBLIC.ACCESS_RESTRICTION(ACCESS_RESTRICTION, UUID, EXPIRED, BLOCKING_REASON) VALUES('locked', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3', FALSE, 'punition3')
-- add User Restrictions
INSERT INTO PUBLIC.USER_RESTRICTIONS(USER_UUID, RESTRICTION_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PUBLIC.USER_RESTRICTIONS(USER_UUID, RESTRICTION_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1')
INSERT INTO PUBLIC.USER_RESTRICTIONS(USER_UUID, RESTRICTION_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2')

/* Add Product Cart ******************************************************************************/
INSERT INTO PUBLIC.PRODUCTCARTS(UUID, USER_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PUBLIC.PRODUCTCARTS(UUID, USER_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3')
INSERT INTO PUBLIC.PRODUCTCARTS(UUID, USER_UUID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2')
-- add product in cart
INSERT INTO PUBLIC.CART_PRODUCTS(CART_UUID, PRODUCT_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 0)
INSERT INTO PUBLIC.CART_PRODUCTS(CART_UUID, PRODUCT_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 5)
INSERT INTO PUBLIC.CART_PRODUCTS(CART_UUID, PRODUCT_ID) VALUES('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 5)

/* Add NetworkUsage ******************************************************************************/
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(0, '2014-07-10 17:00:00.000000', TRUE, 2, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(1, '2014-07-10 17:00:00.000000', TRUE, 4, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(2, '2014-07-12 17:00:00.000000', FALSE, 8, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(3, '2014-07-13 17:00:00.000000', TRUE, 16, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(4, '2014-07-14 17:00:00.000000', FALSE, 32, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(5, '2014-07-16 17:00:00.000000', TRUE, 64, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(6, '2014-07-16 17:00:00.000000', FALSE, 128, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3');
INSERT INTO PUBLIC.NETWORK_USAGE(ID, DATE, IS_DOWNLOAD, SIZE, USER_UUID) VALUES(7, '2014-07-16 17:01:00.000000', TRUE, 512, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2');

/* Add Orders ************************************************************************************/
INSERT INTO PUBLIC.ORDERS(DATASTORE_NAME, PRODUCT_UUID, JOB_ID, STATUS, SUBMISSION_TIME, ESTIMATED_TIME, STATUS_MESSAGE) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'foo', 'COMPLETED', NOW(), NULL, 'requested product is available')
INSERT INTO PUBLIC.ORDERS(DATASTORE_NAME, PRODUCT_UUID, JOB_ID, STATUS, SUBMISSION_TIME, ESTIMATED_TIME, STATUS_MESSAGE) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'bar', 'COMPLETED', NOW(), NULL, 'requested product is available')
INSERT INTO PUBLIC.ORDERS(DATASTORE_NAME, PRODUCT_UUID, JOB_ID, STATUS, SUBMISSION_TIME, ESTIMATED_TIME, STATUS_MESSAGE) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'baz',   'PENDING', NOW(), NULL, NULL)
-- add owners
INSERT INTO PUBLIC.ORDER_OWNERS(DATASTORE_NAME, PRODUCT_UUID, USER_UUID) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PUBLIC.ORDER_OWNERS(DATASTORE_NAME, PRODUCT_UUID, USER_UUID) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1')
INSERT INTO PUBLIC.ORDER_OWNERS(DATASTORE_NAME, PRODUCT_UUID, USER_UUID) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2')

INSERT INTO PUBLIC.ORDER_OWNERS(DATASTORE_NAME, PRODUCT_UUID, USER_UUID) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')

INSERT INTO PUBLIC.ORDER_OWNERS(DATASTORE_NAME, PRODUCT_UUID, USER_UUID) VALUES('storename', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')

/* Add Transformations ***************************************************************************/
INSERT INTO PUBLIC.TRANSFORMATIONS(UUID, CREATION_DATE, TRANSFORMER, PARAMETERS_HASH, PRODUCT_IN, STATUS) VALUES('ttttttttttttttttttttttttttttttt0', NOW(), 'transformer0', 0, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0', 'PENDING')
INSERT INTO PUBLIC.TRANSFORMATIONS(UUID, CREATION_DATE, TRANSFORMER, PARAMETERS_HASH, PRODUCT_IN, STATUS) VALUES('ttttttttttttttttttttttttttttttt1', NOW(), 'transformer1', 0, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1', 'PENDING')
INSERT INTO PUBLIC.TRANSFORMATIONS(UUID, CREATION_DATE, TRANSFORMER, PARAMETERS_HASH, PRODUCT_IN, PRODUCT_OUT, STATUS) VALUES('ttttttttttttttttttttttttttttttt2', NOW(), 'transformer1', 0, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa4', 'COMPLETED')
-- add owners
INSERT INTO PUBLIC.USER_TRANSFORMATIONS(TRANSFORMATION_UUID, USER_UUID) VALUES('ttttttttttttttttttttttttttttttt0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PUBLIC.USER_TRANSFORMATIONS(TRANSFORMATION_UUID, USER_UUID) VALUES('ttttttttttttttttttttttttttttttt0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1')
INSERT INTO PUBLIC.USER_TRANSFORMATIONS(TRANSFORMATION_UUID, USER_UUID) VALUES('ttttttttttttttttttttttttttttttt0', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2')
INSERT INTO PUBLIC.USER_TRANSFORMATIONS(TRANSFORMATION_UUID, USER_UUID) VALUES('ttttttttttttttttttttttttttttttt1', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
INSERT INTO PUBLIC.USER_TRANSFORMATIONS(TRANSFORMATION_UUID, USER_UUID) VALUES('ttttttttttttttttttttttttttttttt2', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0')
