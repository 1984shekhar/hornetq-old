/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */


package org.hornetq.tests.integration.cluster.distribution;

/**
 * A NettySymmetricClusterWithDiscoveryTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 13 Feb 2009 13:52:03
 *
 *
 */
public class NettySymmetricClusterWithDiscoveryTest extends SymmetricClusterWithDiscoveryTest 
{
   protected boolean isNetty()
   {
      return true;
   }

   protected boolean isFileStorage()
   {
      return true;
   }

}
