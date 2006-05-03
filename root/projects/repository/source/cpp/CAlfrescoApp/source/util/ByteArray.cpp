/*
* Copyright (C) 2005 Alfresco, Inc.
*
* Licensed under the Alfresco Network License. You may obtain a
* copy of the License at
*
*   http://www.alfrescosoftware.com/legal/
*
* Please view the license relevant to your network subscription.
*
* BY CLICKING THE "I UNDERSTAND AND ACCEPT" BOX, OR INSTALLING,  
* READING OR USING ALFRESCO'S Network SOFTWARE (THE "SOFTWARE"),  
* YOU ARE AGREEING ON BEHALF OF THE ENTITY LICENSING THE SOFTWARE    
* ("COMPANY") THAT COMPANY WILL BE BOUND BY AND IS BECOMING A PARTY TO 
* THIS ALFRESCO NETWORK AGREEMENT ("AGREEMENT") AND THAT YOU HAVE THE   
* AUTHORITY TO BIND COMPANY. IF COMPANY DOES NOT AGREE TO ALL OF THE   
* TERMS OF THIS AGREEMENT, DO NOT SELECT THE "I UNDERSTAND AND AGREE"   
* BOX AND DO NOT INSTALL THE SOFTWARE OR VIEW THE SOURCE CODE. COMPANY   
* HAS NOT BECOME A LICENSEE OF, AND IS NOT AUTHORIZED TO USE THE    
* SOFTWARE UNLESS AND UNTIL IT HAS AGREED TO BE BOUND BY THESE LICENSE  
* TERMS. THE "EFFECTIVE DATE" FOR THIS AGREEMENT SHALL BE THE DAY YOU  
* CHECK THE "I UNDERSTAND AND ACCEPT" BOX.
*/

#include "util\ByteArray.h"
#include <memory.h>

using namespace Alfresco;

/**
 * Class constructor
 * 
 * @param len BUFLEN
 * @param clearMem bool
 */
ByteArray::ByteArray( BUFLEN len, bool clearMem) {

	//	Allocate a byte array of the specified size

	if ( len > 0) {
		m_data = new unsigned char[ len];

		if ( clearMem)
			memset( m_data, 0, len);
	}
	else
		m_data   = NULL;
	m_length = len;
}

/**
 * Class constructor
 * 
 * @param data CBUFPTR
 * @param len BUFLEN
 */
ByteArray::ByteArray( CBUFPTR data, BUFLEN len) {
	m_data   = NULL;
	m_length = 0;

	setData( data, len);
}

/**
* Class constructor
* 
* @param data const char*
* @param len BUFLEN
*/
ByteArray::ByteArray( const char* data, BUFLEN len) {
	m_data   = NULL;
	m_length = 0;

	setData(( CBUFPTR) data, len);
}

/**
 * Copy constructor
 * 
 * @param byts const ByteArray&
 */
ByteArray::ByteArray( const ByteArray& byts) {
	m_data   = NULL;
	m_length = 0;

	setData( byts.getData(), byts.getLength());
}

/**
 * Class destructor
 * 
 * @return 
 */
ByteArray::~ByteArray() {
	if ( m_data != NULL)
		delete[] m_data;
}

/**
 * Subscript operator
 * 
 * @param idx const unsigned int
 * @return unsigned char&
 */
unsigned char& ByteArray::operator [](const unsigned int idx) {
	return m_data[ idx];
}

/**
 * Assignment operator
 * 
 * @param byts const ByteArray&
 * @return ByteArray&
 */
ByteArray& ByteArray::operator = (const ByteArray& byts) {
	if ( byts.getLength() > 0)
		setData( byts.getData(), byts.getLength());
	else
		m_length = 0;

	return *this;
}

/**
 * Assignment operator
 * 
 * @param byts std::string&
 * @return ByteArray&
 */
ByteArray& ByteArray::operator = ( std::string& byts) {
	if ( byts.length() > 0)
		setData(( CBUFPTR) byts.data(), ( BUFLEN) byts.length());
	else
		m_length = 0;

	return *this;
}

/**
 * Equality operator
 * 
 * @param byts const ByteArray&
 * @return bool
 */
bool ByteArray::operator== ( const ByteArray& byts) {

	//	Check if the arrays are the same length

	if ( getLength() != byts.getLength())
		return false;

	//	Check if the array is empty

	if (getLength() == 0)
		return true;

	//	Check if the array bytes are equal

	if ( memcmp( getData(), byts.getData(), getLength()) == 0)
		return true;
	return false;
}

/**
 * Set the array length, and optionally clear the memory
 * 
 * @param len BUFLEN
 * @param clearMem bool
 */
void ByteArray::setLength( BUFLEN len, bool clearMem) {

	//	Check if the current block is the correct length

	if ( m_length != len) {

		//	Delete the current array

		if ( m_data != NULL)
			delete[] m_data;

		//	Allocate the new array

		if ( len > 0)
			m_data   = new unsigned char[len];
		else
			m_data = NULL;
		m_length = len;
	}

	//	Check if the memory should be cleared

	if ( clearMem && m_data != NULL)
		memset( m_data, 0, m_length);
}

/**
 * Set the data and length
 * 
 * @param data CBUFPTR
 * @param len BUFLEN
 */
void ByteArray::setData( CBUFPTR data, BUFLEN len) {

	//	Delete the existing data

	if ( m_data != NULL)
		delete[] m_data;

	//	Allocate a byte array of the specified size

	if ( data != NULL && len > 0) {

		//	Allocate a buffer and copy the data

		m_data   = new unsigned char[ len];
		memcpy( m_data, data, len);
	}
	else
		m_data = NULL;
	m_length = len;
}

/**
 * Set a byte value
 * 
 * @param idx unsigned int
 * @param val unsigned char
 */
void ByteArray::setByte( unsigned int idx, unsigned char val) {
	if ( idx < getLength())
		m_data[idx] = val;
}
