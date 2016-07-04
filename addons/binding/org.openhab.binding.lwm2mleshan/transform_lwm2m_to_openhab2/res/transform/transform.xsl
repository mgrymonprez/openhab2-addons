<xsl:stylesheet version="2.0"
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:msxsl="urn:schemas-microsoft-com:xslt"
				xmlns:thing-description="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0"
				xmlns:config-description="http://eclipse.org/smarthome/schemas/config-description/v1.0.0"
                exclude-result-prefixes="msxsl">

	<xsl:strip-space elements="*"/>
	<xsl:output omit-xml-declaration="no" indent="yes"/>
				
  <xsl:template match="/*">
    <xsl:apply-templates select="node()"/>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="Object">
	<xsl:result-document method="xml" href="thing-id{ObjectID}.xml">
    <thing:thing-descriptions bindingId="lwm2mleshan"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:thing="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0"
        xsi:schemaLocation="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0 http://eclipse.org/smarthome/schemas/thing-description-1.0.0.xsd">
			
		<xsl:element name="thing-type">
			<xsl:attribute name="id">
				<xsl:value-of select="self::node()/ObjectID" />
			</xsl:attribute>
			
			<supported-bridge-type-refs>
			  <bridge-type-ref id="lwm2mBridgeThing" />
			</supported-bridge-type-refs>
			
			<label><xsl:value-of select="self::node()/Name" /></label>
			
			<description>
				<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
				<xsl:value-of select="self::node()/Description1" />
				<xsl:value-of select="self::node()/Description2" />
				<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
			</description>
			
			<channels>
			<xsl:for-each select="Resources/Item">
				<xsl:variable name="name" select="replace(replace(self::node()/Name, ' ', ''), '/', '_')" />
				<xsl:variable name="itemid" select="self::node()/@ID" />
				<xsl:element name="channel">
					<xsl:attribute name="id">
						<xsl:value-of select="$name" />
					</xsl:attribute>
					<xsl:attribute name="typeId">
						<xsl:value-of select="$itemid" />
					</xsl:attribute>
				</xsl:element>
			</xsl:for-each>
			</channels>
			
			<properties>
				<property name="ObjectID"><xsl:value-of select="self::node()/ObjectID" /></property>
				<property name="ObjectURN"><xsl:value-of select="self::node()/ObjectURN" /></property>
				<property name="MultipleInstances"><xsl:value-of select="self::node()/MultipleInstances" /></property>
				<property name="Mandatory"><xsl:value-of select="self::node()/Mandatory" /></property>
			</properties>
		
		</xsl:element> <!--thing-type-->
	</thing:thing-descriptions>
	</xsl:result-document>
	
	<xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="Object/Resources/Item">
	<xsl:variable name="itemid" select="self::node()/@ID" />
	<xsl:result-document method="xml" href="channel-id{$itemid}.xml">
	
	<thing:thing-descriptions bindingId="lwm2mleshan"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:thing="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0"
		xsi:schemaLocation="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0 http://eclipse.org/smarthome/schemas/thing-description-1.0.0.xsd">
	<xsl:element name="channel-type">
		<xsl:attribute name="id">
			<xsl:value-of select="$itemid" />
		</xsl:attribute>
		<xsl:if test="self::node()/Mandatory='Optional'">
			<xsl:attribute name="advanced"><xsl:text>true</xsl:text></xsl:attribute>
		</xsl:if>
		
		<!-- item-type: Switch, Rollershutter, Contact, String, Number, Dimmer, DateTime, Color, Image -->
		<xsl:choose>
         <xsl:when test="self::node()/Operations='E'">
			<item-type>Switch</item-type>
         </xsl:when>
         <xsl:otherwise>
			<xsl:choose>
				<xsl:when test="self::node()/Type='Boolean'">
					<xsl:choose>
						 <xsl:when test="self::node()/Operations='R'">
							<item-type>Contact</item-type>
						 </xsl:when>
						 <xsl:otherwise>
							<item-type>Switch</item-type>
						 </xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:when test="self::node()/Type='Integer'">
					<xsl:choose>
						 <xsl:when test="$itemid='5851'">
							<item-type>Dimmer</item-type>
						 </xsl:when>
						 <xsl:otherwise>
							<item-type>Number</item-type>
						 </xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:when test="self::node()/Type='Float'">
					<item-type>Number</item-type>
				</xsl:when>
				<xsl:when test="self::node()/Type='Opaque'">
					<item-type>String</item-type>
				</xsl:when>
				<xsl:when test="self::node()/Type='String'">
					<xsl:choose>
						 <xsl:when test="$itemid='5706'">
							<item-type>Color</item-type>
						 </xsl:when>
						 <xsl:otherwise>
							<item-type>String</item-type>
						 </xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:when test="self::node()/Type='Time'">
					<item-type>DateTime</item-type>
				</xsl:when>
				<xsl:otherwise>
					<xsl:message terminate="yes">Type not known</xsl:message>
				</xsl:otherwise>
			</xsl:choose>
         </xsl:otherwise>
       </xsl:choose>

		<label><xsl:value-of select="self::node()/Name" /></label>
		
		<description>
			<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
			<xsl:value-of select="self::node()/Description" />
			<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
		</description>
		
		<!-- category -->
		<xsl:if test="self::node()/Type='Boolean'">
			<category>
			<xsl:choose>
				 <xsl:when test="self::node()/Operations='R'">
					Sensor
				 </xsl:when>
				 <xsl:otherwise>
					Switch
				 </xsl:otherwise>
			</xsl:choose>
			</category>
		</xsl:if>
		
		<!-- tags -->
		 <xsl:if test="self::node()/Operations='E'">
            <tags>
				<tag>Executable</tag>
			</tags>
         </xsl:if>
		 
		<!-- state -->
		<xsl:if test="self::node()/Operations='R'">
			<state readOnly="true"></state>
		</xsl:if>
		
		<xsl:if test="self::node()/Type='Integer' and $itemid='5851'">
			<state min="0" max="100"></state>
		</xsl:if>
		
	</xsl:element> <!--channel-type-->
	</thing:thing-descriptions>
    </xsl:result-document>
  </xsl:template>

</xsl:stylesheet>
