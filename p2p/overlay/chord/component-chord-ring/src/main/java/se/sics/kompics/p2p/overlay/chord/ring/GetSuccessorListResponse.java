/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.p2p.overlay.chord.ring;

import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordMessage;

/**
 * The <code>GetSuccessorListResponse</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class GetSuccessorListResponse extends ChordMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1250507659405683406L;

	private final SuccessorList successorList;

	private final RequestState requestState;

	public GetSuccessorListResponse(SuccessorList successorList,
			RequestState requestState, ChordAddress source,
			ChordAddress destination) {
		super(source, destination);
		this.successorList = successorList;
		this.requestState = requestState;
	}

	public SuccessorList getSuccessorList() {
		return successorList;
	}

	public RequestState getRequestState() {
		return requestState;
	}
}
