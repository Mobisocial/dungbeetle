/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

public interface IdentityProvider {
	public String userName();
	public String userEmail();
	public String userProfile();
	public RSAPublicKey userPublicKey();
	public RSAPrivateKey userPrivateKey();
	public String userPersonId();
	public RSAPublicKey publicKeyForPersonId(String id);
	public List<RSAPublicKey> publicKeysForContactIds(List<Long> ids);
	public String personIdForPublicKey(RSAPublicKey key);
	public void close();
}
