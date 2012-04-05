/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.webconsole.previewprocessor;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@Component
@Service
@Properties({
  @Property(name = WebConsoleConstants.PLUGIN_LABEL, value = "previewprocessor")
})
public class PreviewProcessorWebConsolePlugin extends SimpleWebConsolePlugin {
  private static final long serialVersionUID = 1L;

  private final String TEMPLATE;

  public PreviewProcessorWebConsolePlugin() {
    super("previewprocessor", "%plugin_title", new String[] { "/dev/css/sakai/main.css" });

    TEMPLATE = readTemplateFile("/templates/previewprocessor.html");
  }

  @Override
  @Activate @Modified
  public void activate(BundleContext bundleContext) {
    super.activate(bundleContext);
  }

  @Override
  @Deactivate
  public void deactivate() {
    super.deactivate();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    super.doGet(req, resp);
  }

  @Override
  protected void renderContent(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    String type = req.getParameter("type");
    if (type.equals("all")){
      //TODO: get list of all possible uploaded documents, that needs PP
      //TODO: Work to bring Preview Processor to java is still on-going
      //PreviewProcessorService.generatePreview(id);
    } else {
      String contentIdString = req.getParameter("content_ids");
      String[] contentIds = null;
      if (contentIdString != null){
        contentIds = contentIdString.split(",");
      }
      for (int id = 0; id < contentIds.length; id++){
        //TODO: Work to bring Preview Processor to java is still on-going
        //PreviewProcessorService.generatePreview(id);
      }
    }
    PrintWriter writer = res.getWriter();
    writer.write(TEMPLATE);
    writer.write("<pre id='output'>");
    //TODO: Output from PreviewProcessorService
    writer.write("</pre>");  
  }
}
