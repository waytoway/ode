/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.bpel.memdao;

import org.apache.ode.bpel.dao.ContextValueDAO;

public class ContextValueDAOImpl extends DaoBaseImpl implements ContextValueDAO {

    private String _namespace;
	private String _key;
    private String _value;

    public ContextValueDAOImpl() {}
    public ContextValueDAOImpl(String namespace, String key){
        _namespace = namespace;
        _key = key;
    }

    public String getKey() {
        return _key;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        _value = value;
    }

	public String getNamespace() {
		return _namespace;
	}
	
	public void setKey(String key) {
		_key = key;
	}
	
	public void setNamespace(String namespace) {
		_namespace = namespace;
	}

}
