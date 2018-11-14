/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.android.tools.build.bundletool.xml;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.build.bundletool.utils.xmlproto.XmlProtoAttributeBuilder;
import com.android.tools.build.bundletool.utils.xmlproto.XmlProtoElement;
import com.android.tools.build.bundletool.utils.xmlproto.XmlProtoElementBuilder;
import com.android.tools.build.bundletool.utils.xmlproto.XmlProtoNode;
import com.google.common.base.Joiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;

@RunWith(JUnit4.class)
public final class XmlProtoToXmlConverterTest {

  private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
  private static final String DIST_NS = "http://schemas.android.com/apk/distribution";

  private static final String LINE_BREAK = System.lineSeparator();
  private static final Joiner LINE_BREAK_JOINER = Joiner.on(LINE_BREAK);

  @Test
  public void testConversionManifest() {
    XmlProtoElement manifestElement =
        XmlProtoElementBuilder.create("manifest")
            .addNamespaceDeclaration("android", ANDROID_NS)
            .addNamespaceDeclaration("dist", DIST_NS)
            .addAttribute(newAttribute("package").setValueAsString("com.example.app"))
            .addAttribute(newAttribute(ANDROID_NS, "versionCode").setValueAsDecimalInteger(123))
            .addAttribute(newAttribute(ANDROID_NS, "versionName").setValueAsString("1.2.3"))
            .addChildElement(
                XmlProtoElementBuilder.create(DIST_NS, "module")
                    .addAttribute(newAttribute(DIST_NS, "onDemand").setValueAsBoolean(true))
                    .addAttribute(
                        newAttribute(DIST_NS, "title")
                            .setValueAsRefId(0x7f0b0001, "string/title_module")))
            .addChildElement(
                XmlProtoElementBuilder.create("application")
                    .addAttribute(newAttribute(ANDROID_NS, "name").setValueAsString(".MyApp"))
                    .addAttribute(
                        newAttribute(ANDROID_NS, "icon")
                            .setValueAsRefId(0x7f010005, "mipmap/ic_launcher"))
                    .addAttribute(newAttribute(ANDROID_NS, "supportsRtl").setValueAsBoolean(true))
                    .addChildElement(
                        XmlProtoElementBuilder.create("meta-data")
                            .addAttribute(
                                newAttribute(ANDROID_NS, "name")
                                    .setValueAsString("com.google.android.gms.version"))
                            .addAttribute(
                                newAttribute(ANDROID_NS, "value")
                                    .setValueAsRefId(
                                        0x7f0b0002, "integer/google_play_services_version")))
                    .addChildElement(
                        XmlProtoElementBuilder.create("activity")
                            .addAttribute(
                                newAttribute(ANDROID_NS, "exported").setValueAsBoolean(true))
                            .addAttribute(
                                newAttribute(ANDROID_NS, "name")
                                    .setValueAsString("com.example.app.MyActivity"))))
            .build();

    XmlProtoNode manifestProto = XmlProtoNode.createElementNode(manifestElement);

    Document manifestXml = XmlProtoToXmlConverter.convert(manifestProto);

    assertThat(XmlUtils.documentToString(manifestXml))
        .isEqualTo(
            LINE_BREAK_JOINER.join(
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                    + "xmlns:dist=\"http://schemas.android.com/apk/distribution\" "
                    + "android:versionCode=\"123\" "
                    + "android:versionName=\"1.2.3\" "
                    + "package=\"com.example.app\">",
                "  <dist:module dist:onDemand=\"true\" dist:title=\"@string/title_module\"/>",
                "  <application android:icon=\"@mipmap/ic_launcher\" "
                    + "android:name=\".MyApp\" "
                    + "android:supportsRtl=\"true\">",
                "    <meta-data android:name=\"com.google.android.gms.version\" "
                    + "android:value=\"@integer/google_play_services_version\"/>",
                "    <activity android:exported=\"true\" "
                    + "android:name=\"com.example.app.MyActivity\"/>",
                "  </application>",
                "</manifest>" + LINE_BREAK));
  }

  @Test
  public void testNamespaceScopes() {
    XmlProtoNode proto =
        XmlProtoNode.createElementNode(
            XmlProtoElementBuilder.create("root")
                .addChildElement(
                    XmlProtoElementBuilder.create("child1")
                        .addNamespaceDeclaration("a", "http://uri")
                        .addAttribute(newAttribute("http://uri", "test1").setValueAsBoolean(true)))
                .addChildElement(
                    XmlProtoElementBuilder.create("child2")
                        .addNamespaceDeclaration("b", "http://uri")
                        .addAttribute(newAttribute("http://uri", "test2").setValueAsBoolean(true)))
                .build());

    Document document = XmlProtoToXmlConverter.convert(proto);
    String xmlString = XmlUtils.documentToString(document);

    assertThat(xmlString)
        .isEqualTo(
            LINE_BREAK_JOINER.join(
                "<root>",
                "  <child1 xmlns:a=\"http://uri\" a:test1=\"true\"/>",
                "  <child2 xmlns:b=\"http://uri\" b:test2=\"true\"/>",
                "</root>" + LINE_BREAK));
  }

  @Test
  public void testRedundantNamespaces_usesLastOneFound() {
    XmlProtoNode proto =
        XmlProtoNode.createElementNode(
            XmlProtoElementBuilder.create("root")
                .addNamespaceDeclaration("a", "http://uri")
                .addChildElement(
                    XmlProtoElementBuilder.create("child1")
                        .addNamespaceDeclaration("b", "http://uri")
                        .addAttribute(newAttribute("http://uri", "test1").setValueAsBoolean(true)))
                .addChildElement(
                    XmlProtoElementBuilder.create("child2")
                        .addAttribute(newAttribute("http://uri", "test2").setValueAsBoolean(true)))
                .build());

    Document document = XmlProtoToXmlConverter.convert(proto);
    String xmlString = XmlUtils.documentToString(document);

    assertThat(xmlString)
        .isEqualTo(
            LINE_BREAK_JOINER.join(
                "<root xmlns:a=\"http://uri\">",
                "  <child1 xmlns:b=\"http://uri\" b:test1=\"true\"/>",
                "  <child2 a:test2=\"true\"/>",
                "</root>" + LINE_BREAK));
  }

  private static XmlProtoAttributeBuilder newAttribute(String attrName) {
    return XmlProtoAttributeBuilder.create(attrName);
  }

  private static XmlProtoAttributeBuilder newAttribute(String namespace, String attrName) {
    return XmlProtoAttributeBuilder.create(namespace, attrName);
  }
}
