/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.HttpConnection;
import org.apache.http.io.HttpDataReceiver;
import org.apache.http.io.HttpDataTransmitter;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * <p>
 * </p>
 * @author <a href="mailto:oleg at ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision$
 * 
 * @since 4.0
 */
abstract class AbstractHttpConnection implements HttpConnection {

    protected transient Socket socket = null;
    protected transient HttpDataTransmitter datatransmitter = null;
    protected transient HttpDataReceiver datareceiver = null;
    
    protected AbstractHttpConnection() {
        super();
    }
    
    protected void assertNotOpen() {
        if (this.socket != null) {
            throw new IllegalStateException("Connection is already open");
        }
    }
    
    protected void assertOpen() {
        if (this.socket == null) {
            throw new IllegalStateException("Connection is not open");
        }
    }
    
    protected void bind(final Socket socket, final HttpParams params) throws IOException {
        if (socket == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        assertNotOpen();
        HttpConnectionParams connParams = new HttpConnectionParams(params); 
        this.socket.setTcpNoDelay(connParams.getTcpNoDelay());
        this.socket.setSoTimeout(connParams.getSoTimeout());
        
        int linger = connParams.getLinger();
        if (linger >= 0) {
            this.socket.setSoLinger(linger > 0, linger);
        }
        
        int sndBufSize = connParams.getSendBufferSize();
        if (sndBufSize >= 0) {
            this.socket.setSendBufferSize(sndBufSize);
        }        
        int rcvBufSize = connParams.getReceiveBufferSize();
        if (rcvBufSize >= 0) {
            this.socket.setReceiveBufferSize(rcvBufSize);
        }
        this.datatransmitter = 
            new DefaultHttpDataTransmitterFactory().create(this.socket); 
        this.datareceiver = 
            new DefaultHttpDataReceiverFactory().create(this.socket); 
    }

    public boolean isOpen() {
        return this.socket != null;
    }
    
    public void close() throws IOException {
        HttpDataTransmitter tmp1 = this.datatransmitter;
        if (tmp1 != null) {
            tmp1.flush();
        }
        this.datareceiver = null;
        this.datatransmitter = null;
        Socket tmp2 = this.socket;
        if (tmp2 != null) {
            tmp2.close();
        }
        this.socket = null;
    }
    
    public int getSocketTimeout() throws IOException {
        assertOpen();
        return this.socket.getSoTimeout();
    }
    
    
    public void setSocketTimeout(int timeout) throws IOException {
        assertOpen();
        this.socket.setSoTimeout(timeout);
    }
    
    public boolean isStale() {
        assertOpen();
        try {
            this.datareceiver.isDataAvailable(1);
            return false;
        } catch (IOException ex) {
            return true;
        }
    }
    
}
